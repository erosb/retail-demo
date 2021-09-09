package org.hazelcast.retaildemo.paymentservice;

import com.hazelcast.aggregation.Aggregators;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.hazelcast.retaildemo.Product;
import org.hazelcast.retaildemo.PaymentFinishedModel;
import org.hazelcast.retaildemo.PaymentRequestModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootApplication
@Slf4j
public class PaymentService {

    @AllArgsConstructor
    @Value
    @Builder
    private static class PayableOrderLine  {

        String productId;
        String description;
        int unitPrice;
        int quantity;
        int totalPrice;

    }

    public static final String PAYMENT_FINISHED = "payment-finished";

    public static void main(String[] args) {
        SpringApplication.run(PaymentService.class);
    }

    @Autowired
    private KafkaTemplate<String, PaymentFinishedModel> kafkaTemplate;

    private final HazelcastInstance hzClient = HazelcastClient.newHazelcastClient();

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {

        };
    }

    @KafkaListener(topics = {"payment-request"}, groupId = "test")
    void newOrderArrived(PaymentRequestModel paymentRequest) {
        log.info("received payment request {}", paymentRequest);

        calculatePayableOrder(paymentRequest);

        String randomTransactionId = randomTransactionId();
        kafkaTemplate.send(PAYMENT_FINISHED, paymentRequest.getOrderId().toString(), PaymentFinishedModel.builder()
                .orderId(paymentRequest.getOrderId())
                .isSuccess(randomSuccessOrFailure())
                        .transactionId(randomTransactionId)
                        .invoiceDocUrl("S3://retail-demo/invoice-" + randomTransactionId + ".pdf")
                .build());
    }

    private static String randomTransactionId() {
        return IntStream.range(0, 16).mapToObj(__ -> Math.random())
                .map(rnd -> '0' + ((int)(rnd * 10)))
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    private void calculatePayableOrder(PaymentRequestModel paymentRequest) {
        IMap<String, Product> products = hzClient.getMap("products");
        paymentRequest.getOrderLines().stream()
                .map(line -> {
                    Product product = products.get(line.getProductId());
                    return PayableOrderLine.builder()
                            .productId(product.getProductId())
                            .description(product.getDescription())
                            .unitPrice(product.getUnitPrice())
                            .quantity(line.getQuantity())
                            .totalPrice(product.getUnitPrice() * line.getQuantity())
                            .build();
                })
                .forEach(System.out::println);
    }

    private boolean randomSuccessOrFailure() {
        return Math.random() > 0.15d;
    }

}
