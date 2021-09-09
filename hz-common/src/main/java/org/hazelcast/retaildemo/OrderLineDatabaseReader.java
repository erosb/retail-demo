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

    public List<OrderLineModel> findOrderLinesByOrderId(Long orderId) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM order_line WHERE order_id = ?")) {
            ps.setLong(1, orderId);
            ResultSet rs = ps.executeQuery();
            List<OrderLineModel> rval = new ArrayList<>();
            while (rs.next()) {
                rval.add(OrderLineModel.builder()
                                .productId(rs.getString("product_id"))
                                .quantity(rs.getInt("quantity"))
                        .build());
            }
            return rval;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
