package com.hazelcast.retaildemo.stockservice;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.retaildemo.sharedmodels.AddressModel;
import com.hazelcast.retaildemo.sharedmodels.OrderLineModel;
import com.hazelcast.retaildemo.sharedmodels.OrderModel;
import com.hazelcast.retaildemo.sharedmodels.PaymentFinishedModel;
import com.hazelcast.retaildemo.sharedmodels.PaymentRequestModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

@SpringBootApplication
@Slf4j
public class StockService {

    public static void main(String[] args) {
        SpringApplication.run(StockService.class, args);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
    private KafkaTemplate<String, PaymentRequestModel> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @KafkaListener(topics = "new-orders", groupId = "test")
    public void newOrder(OrderModel order) {
        log.info("received new-orders: {}", order);
        txTemplate.executeWithoutResult(status ->
                order.getOrderLines().forEach(line -> {
                    Long avail = jdbcTemplate.queryForObject("SELECT available_quantity FROM stock WHERE product_id = ?",
                            Long.class,
                            line.getProductId());
                    if (avail == 0L) {
                        // error handling
                        return;
                    }
                    jdbcTemplate.update("UPDATE stock SET "
                                    + "reserved_quantity = reserved_quantity + ?, "
                                    + "available_quantity = available_quantity - ? "
                                    + "WHERE product_id = ?",
                            line.getQuantity(),
                            line.getQuantity(),
                            line.getProductId());
                }));
        Long orderId = orderRepository.save(order);
        kafkaTemplate.send("payment-request", PaymentRequestModel.builder()
                .orderId(orderId)
                .orderLines(order.getOrderLines())
                .build());
    }

    @KafkaListener(topics = "payment-finished", groupId = "test")
    public void paymentFinished(PaymentFinishedModel paymentFinished) {
        log.info("received paymentFinished: {}", paymentFinished);
        Long orderId = paymentFinished.getOrderId();
        var orderLines = orderRepository.findOrderLinesByOrderId(orderId);

        txTemplate.execute(status -> {
            orderLines.forEach(line -> {
                jdbcTemplate.update("UPDATE stock SET reserved_quantity = reserved_quantity - ? "
                                + "WHERE product_id = ?",
                        line.getQuantity(),
                        line.getProductId());
                if (!paymentFinished.isSuccess()) {
                    jdbcTemplate.update("UPDATE stock SET available_quantity = available_quantity + ? "
                                    + "WHERE product_id = ?",
                            line.getQuantity(),
                            line.getProductId());
                }
            });
            return null;
        });
    }
}
