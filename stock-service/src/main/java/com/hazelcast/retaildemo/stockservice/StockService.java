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
import java.util.List;
import java.util.Optional;

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
    private AddressRepository addressRepo;

    private final HazelcastInstance hzClient = HazelcastClient.newHazelcastClient();

    @KafkaListener(topics = "new-orders", groupId = "test")
    public void newOrder(OrderModel order) {
        log.info("received new-orders: {}", order);
        txTemplate.executeWithoutResult(status ->
                order.getOrderLines().forEach(line -> {
                    Long avail = jdbcTemplate.queryForObject("select available_quantity from stock where product_id = ?",
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
        Long orderId = persistOrder(order);
        kafkaTemplate.send("payment-request", PaymentRequestModel.builder()
                .orderId(orderId)
                .orderLines(order.getOrderLines())
                .build());
    }

    @KafkaListener(topics = "payment-finished", groupId = "test")
    public void paymentFinished(PaymentFinishedModel paymentFinished) {
        log.info("received paymentFinished: {}", paymentFinished);
        Long orderId = paymentFinished.getOrderId();
        var orderLines = findOrderLinesByOrderId(orderId);
        txTemplate.execute(status -> {
            orderLines.forEach(line -> {
                jdbcTemplate.update("UPDATE stock SET reserved_quantity = reserved_quantity - ? "
                                + "WHERE product_id = ?",
                        line.getQuantity(),
                        line.getProductId());
            });
            return null;
        });
    }

    private List<OrderLineModel> findOrderLinesByOrderId(Long orderId) {
        return jdbcTemplate.query("SELECT * FROM order_line WHERE order_id = ?",
                (ResultSet rs, int rowNum) -> OrderLineModel.builder()
                        .productId(rs.getString("product_id"))
                        .quantity(rs.getInt("quantity"))
                        .build()
                , orderId);
    }

    private Long persistOrder(OrderModel order) {
        Long invoiceAddressId = Optional.ofNullable(order.getInvoiceAddress()).map(addressRepo::save).map(
                AddressModel::getId).orElse(null);
        Long shippingAddressId = Optional.ofNullable(order.getShippingAddress()).map(addressRepo::save).map(
                AddressModel::getId).orElse(null);
        return txTemplate.execute(status -> {
            GeneratedKeyHolder orderIdHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(conn -> {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO \"order\" (shipping_address_id, invoice_address_id) VALUES (?, ?)");
                ps.setObject(1, shippingAddressId);
                ps.setObject(2, invoiceAddressId);
                return ps;
            }, orderIdHolder);
            Long orderId = orderIdHolder.getKeyAs(Long.class);
            order.getOrderLines().forEach(line -> {
                jdbcTemplate.update("INSERT INTO order_line (order_id, product_id, quantity) VALUES (?, ?, ?)",
                        orderId, line.getProductId(), line.getQuantity());
            });
            return orderId;
        });
    }
}
