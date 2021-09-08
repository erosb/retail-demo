package org.hazelcast.retaildemo.stockservice;

import org.hazelcast.retaildemo.sharedmodels.AddressModel;
import org.hazelcast.retaildemo.sharedmodels.OrderLineModel;
import org.hazelcast.retaildemo.sharedmodels.OrderModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

@Repository
@Slf4j
public class OrderRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate txTemplate;


    @Autowired
    private AddressRepository addressRepo;

    public Long save(OrderModel order) {
        Long invoiceAddressId = Optional.ofNullable(order.getInvoiceAddress()).map(addressRepo::save).map(
                AddressModel::getId).orElse(null);
        Long shippingAddressId = Optional.ofNullable(order.getShippingAddress()).map(addressRepo::save).map(
                AddressModel::getId).orElse(null);
        return txTemplate.execute(status -> {
            GeneratedKeyHolder orderIdHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(conn -> {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO \"order\" (shipping_address_id, invoice_address_id) VALUES (?, ?)", RETURN_GENERATED_KEYS);
                ps.setObject(1, shippingAddressId);
                ps.setObject(2, invoiceAddressId);
                return ps;
            }, orderIdHolder);
            Long orderId = (Long) orderIdHolder.getKeys().get("order_id");
            log.info("persisted order with id={}", orderId);
            order.getOrderLines().forEach(line -> {
                jdbcTemplate.update("INSERT INTO order_line (order_id, product_id, quantity) VALUES (?, ?, ?)",
                        orderId, line.getProductId(), line.getQuantity());
            });
            return orderId;
        });
    }


    public List<OrderLineModel> findOrderLinesByOrderId(Long orderId) {
        return jdbcTemplate.query("SELECT * FROM order_line WHERE order_id = ?",
                (ResultSet rs, int rowNum) -> OrderLineModel.builder()
                        .productId(rs.getString("product_id"))
                        .quantity(rs.getInt("quantity"))
                        .build()
                , orderId);
    }
}
