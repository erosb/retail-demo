package org.hazelcast.retaildemo.stockservice;

import org.hazelcast.retaildemo.sharedmodels.OrderModel;
import org.hazelcast.retaildemo.sharedmodels.PaymentFinishedModel;
import org.hazelcast.retaildemo.sharedmodels.PaymentRequestModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
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
                    if (avail < line.getQuantity()) {
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
