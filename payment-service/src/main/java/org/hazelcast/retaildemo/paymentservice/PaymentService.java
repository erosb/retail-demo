package org.hazelcast.retaildemo.paymentservice;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.hazelcast.retaildemo.Product;
import org.hazelcast.retaildemo.sharedmodels.PaymentFinishedModel;
import org.hazelcast.retaildemo.sharedmodels.PaymentRequestModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

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

        kafkaTemplate.send(PAYMENT_FINISHED, PaymentFinishedModel.builder()
                .orderId(paymentRequest.getOrderId())
                .isSuccess(randomSuccessOrFailure())
                .build());
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
