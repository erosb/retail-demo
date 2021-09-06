package com.hazelcast.retaildemo.stockservice;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.retaildemo.sharedmodels.OrderModel;
import com.hazelcast.retaildemo.sharedmodels.PaymentFinishedModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;

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
    private KafkaTemplate<String, OrderModel> kafkaTemplate;

    private final HazelcastInstance hzClient = HazelcastClient.newHazelcastClient();

    @KafkaListener(topics = "new-orders", groupId = "test")
    public void newOrder(OrderModel order) {
        log.info("received new-orders: {}", order);
        txTemplate.execute(status -> {
//            jdbcTemplate.execute("select available_quantity from stock where product_id = ?",
//                    (PreparedStatementCallback<Object>) prepStmt -> {
//                        prepStmt.setString(1, order.getOrderLines().get(0).getProductId());
//                        return prepStmt.executeQuery().getLong("available_quantity");
//                    });
//            jdbcTemplate.execute((StatementCallback<Object>) stmt -> stmt.executeQuery("select available_quantity from stock where product_id").getString(0));
            order.getOrderLines().forEach(line -> {
                Long avail = jdbcTemplate.queryForObject("select available_quantity from stock where product_id = ?", Long.class,
                        line.getProductId());
                if (avail == 0L) {
                    return;
                }
                jdbcTemplate.update("UPDATE stock SET "
                                + "reserved_quantity = reserved_quantity + ?, "
                                + "available_quantity = available_quantity - ? "
                                + "WHERE product_id = ?",
                        line.getQuantity(),
                        line.getQuantity(),
                        line.getProductId());
            });
            return null;
        });

        kafkaTemplate.send("payment-request", order);
    }

    @KafkaListener(topics = "payment-finished", groupId = "test")
    public void paymentFinished(PaymentFinishedModel paymentFinished) {
        log.info("received paymentFinished: {}", paymentFinished);
        int orderId = paymentFinished.getOrderId();
        txTemplate.execute(status -> {
            order.getOrderLines().forEach(line -> {
                jdbcTemplate.update("UPDATE stock SET reserved_quantity = reserved_quantity - ? "
                                + "WHERE product_id = ?",
                        line.getQuantity(),
                        line.getProductId());
            });
            return null;
        });
    }
}
