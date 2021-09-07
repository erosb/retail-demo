package org.hazelcast.retaildemo.stockservice;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;

@Data
@Table("stock")
public class StockEntry implements Serializable {
    @Id
    @Column("product_id")
    private String productId;
    private int availableQuantity;
    private int reservedQuantity;
    private int unitPrice;

    public void incAvailable(int quantity) {
        availableQuantity += quantity;
    }

    public void decAvailable(int quantity) {
        availableQuantity -= quantity;
    }

    public void incReserved(int quantity) {
        reservedQuantity += quantity;
    }

    public void decReserved(int quantity) {
        reservedQuantity += quantity;
    }
}
