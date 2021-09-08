package org.hazelcast.retaildemo.stockservice;

import org.hazelcast.retaildemo.StockEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class StockRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Optional<StockEntry> findById(String id) {
        return Optional.ofNullable(
                jdbcTemplate.query("SELECT available_quantity, reserved_quantity FROM stock WHERE product_id = ?",
                        rs -> {
                            if (!rs.next()) {
                                return null;
                            }
                            return StockEntry.builder()
                                    .availableQuantity(rs.getInt("available_quantity"))
                                    .reservedQuantity(rs.getInt("reserved_quantity"))
                                    .productId(id)
                                    .build();
                        }, id));
    }
}
