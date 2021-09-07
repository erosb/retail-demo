package org.hazelcast.retaildemo.stockservice;

import org.hazelcast.retaildemo.StockEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public abstract class StockRepository
        implements CrudRepository<StockEntry, String> {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Optional<StockEntry> findById(String id) {
        return Optional.ofNullable(
                jdbcTemplate.query("SELECT available_quantity, reserved_quantity, unit_price FROM stock WHERE product_id = ?",
                        rs -> {
                            return StockEntry.builder()
                                    .availableQuantity(rs.getInt("available_quantity"))
                                    .reservedQuantity(rs.getInt("reserved_quantity"))
                                    .unitPrice(rs.getInt("unit_price"))
                                    .productId(id)
                                    .build();
                        }, id));
    }
}
