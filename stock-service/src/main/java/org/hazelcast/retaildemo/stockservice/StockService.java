package org.hazelcast.retaildemo.stockservice;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.hazelcast.retaildemo.JetJobSubmitter;
import org.hazelcast.retaildemo.ShippableOrder;
import org.hazelcast.retaildemo.StockEntry;
import org.hazelcast.retaildemo.OrderModel;
import org.hazelcast.retaildemo.PaymentFinishedModel;
import org.hazelcast.retaildemo.PaymentRequestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@Slf4j
public class StockService {

    public static void main(String[] args) {
        SpringApplication.run(StockService.class, args);
    }

    @Autowired
    private KafkaTemplate<String, PaymentRequestModel> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    private final HazelcastInstance hzClient = HazelcastClient.newHazelcastClient();

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);

    @Bean
    public ApplicationRunner appRunner() {
        return args -> {
            Properties properties = new Properties();
            properties.put("key.deserializer", StringDeserializer.class.getName());
            properties.put("value.deserializer", JsonDeserializer.class.getName());
            properties.put("bootstrap.servers", "kafka:9092");
            properties.put("auto.offset.reset", "earliest");

            executor.schedule(new JetJobSubmitter(hzClient, properties), 5, TimeUnit.SECONDS);

            executor.scheduleWithFixedDelay(() -> {
                Predicate<Long, ShippableOrder> pred = Predicates.and(
                        Predicates.equal("shippingAddress.postalCode", 4030),
                        Predicates.greaterThan("orderLines[any].totalPrice", 200)
                );
                IMap<Long, ShippableOrder> shippableOrders = hzClient.getMap("shippable_orders");

                shippableOrders.values(pred)
                        .forEach(order -> System.out.println("matching order found, orderId=" + order.getOrderId()));
            }, 5, 2, TimeUnit.SECONDS);
        };
    }

    @KafkaListener(topics = "new-orders", groupId = "test")
    public void newOrder(OrderModel order) {
//        log.info("received new-orders: {}", order);
        IMap<String, StockEntry> stockMap = hzClient.getMap("stock");
        order.getOrderLines().forEach(line -> {
            int requestedQuantity = line.getQuantity();
            stockMap.executeOnKey(line.getProductId(), new ReservationEntryProcessor(requestedQuantity));
        });
        var orderId = orderRepository.save(order);
        hzClient.getMap("shipping_addresses").put(orderId, order.getShippingAddress());
        kafkaTemplate.send("payment-request", PaymentRequestModel.builder()
                .orderId(orderId)
                .orderLines(order.getOrderLines())
                .build());
    }

    @KafkaListener(topics = "payment-finished", groupId = "test")
    public void paymentFinished(PaymentFinishedModel paymentFinished) {
//        log.info("received paymentFinished: {}", paymentFinished);
        Long orderId = paymentFinished.getOrderId();
        var orderLines = orderRepository.findOrderLinesByOrderId(orderId);
        orderLines.forEach(line -> {
                    IMap<String, StockEntry> stockMap = hzClient.getMap("stock");
                    stockMap.executeOnKey(line.getProductId(), new PaymentFinishedEntryProcessor(
                            paymentFinished.isSuccess(),
                            line.getQuantity())
                    );
                }
        );
    }
}
