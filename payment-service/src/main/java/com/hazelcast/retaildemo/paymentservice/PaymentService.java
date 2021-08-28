package com.hazelcast.retaildemo.paymentservice;

import com.hazelcast.retaildemo.sharedmodels.OrderModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;

import java.io.IOException;

@SpringBootApplication
@Slf4j
public class PaymentService {

    public static void main(String[] args) {
        SpringApplication.run(PaymentService.class);
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {

        };
    }

    @KafkaListener(topics = {"new-orders"}, groupId = "test")
    void newOrderArrived(OrderModel order) {
        log.info("received order {}", order);
    }

}
