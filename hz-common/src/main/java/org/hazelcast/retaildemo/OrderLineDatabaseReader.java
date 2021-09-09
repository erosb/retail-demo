package org.hazelcast.retaildemo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderLineDatabaseReader {

    private final Connection conn;

    public OrderLineDatabaseReader(){
        try {
            conn = DriverManager.getConnection("jdbc:postgresql://db:5432/hz-demo", "postgres", "postgres");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShippableOrderLine> findOrderLinesByOrderId(Long orderId) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT ol.product_id as product_id, quantity, description, unit_price "
                + "FROM order_line ol JOIN products p ON ol.product_id = p.product_id "
                + "WHERE order_id = ?")) {
            ps.setLong(1, orderId);
            ResultSet rs = ps.executeQuery();
            List<ShippableOrderLine> rval = new ArrayList<>();
            while (rs.next()) {
                rval.add(ShippableOrderLine.builder()
                                .productId(rs.getString("product_id"))
                                .quantity(rs.getInt("quantity"))
                                .productDescription(rs.getString("description"))
                                .unitPrice(rs.getInt("unit_price"))
                                .totalPrice(rs.getInt("unit_price") * rs.getInt("quantity"))
                        .build());
            }
            return rval;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
