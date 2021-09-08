package org.hazelcast.retaildemo.webshop;

import org.hazelcast.retaildemo.sharedmodels.OrderLineModel;
import org.hazelcast.retaildemo.sharedmodels.OrderModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@SpringBootApplication
@Slf4j
public class WebshopApplication {

    private static final String NEW_ORDERS = "new-orders";

    private static final List<String> PRODUCT_IDS = List.of(
            "WXS9AG",
            "WXTJN9",
            "O9EX67",
            "EU1LG5",
            "HDQGWJ",
            "KQBWZ7",
            "J3H82O",
            "BPGFYV",
            "5H0SMI",
            "BHRQ8P",
            "F5TY38",
            "H9VH0C",
            "HV547Q",
            "G7R3FQ",
            "JX5GJA",
            "4O8XGO",
            "FXL960",
            "GFR3TV",
            "7W3PSW",
            "9G4OMW"
    );

    public static void main(String[] args) {
        SpringApplication.run(WebshopApplication.class, args);
    }

    @Autowired
    private KafkaTemplate<String, OrderModel> kafkaTemplate;

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            for (; ; ) {
                Thread.sleep(500);
                kafkaTemplate.send(NEW_ORDERS, createRandomOrder());
            }
        };
    }

    private OrderModel createRandomOrder() {
        return OrderModel.builder()
                .orderLines(
                        IntStream.range(0, 2 + (int) (Math.random() * 8))
                                .mapToObj(i -> OrderLineModel.builder()
                                        .productId(PRODUCT_IDS.get((int) (PRODUCT_IDS.size() * Math.random())))
                                        .quantity((int) (Math.random() * 30))
                                        .build()
                                ).collect(toList())
                ).build();
    }
}
