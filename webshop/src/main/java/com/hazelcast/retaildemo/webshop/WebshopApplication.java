package com.hazelcast.retaildemo.webshop;

import com.hazelcast.retaildemo.sharedmodels.OrderLineModel;
import com.hazelcast.retaildemo.sharedmodels.OrderModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

@SpringBootApplication
@Slf4j
public class WebshopApplication {

    private static final String NEW_ORDERS = "new-orders";

    public static void main(String[] args) {
        SpringApplication.run(WebshopApplication.class, args);
    }

    @Autowired
    private KafkaTemplate<String, OrderModel> kafkaTemplate;

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            kafkaTemplate.send(NEW_ORDERS, OrderModel.builder()
                    .orderLines(List.of(OrderLineModel.builder()
                            .productId("rnd1")
                            .quantity(2)
                            .build()))
                    .build());
            log.info("Hello");
        };
    }
}
