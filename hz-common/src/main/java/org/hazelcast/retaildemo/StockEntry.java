package org.hazelcast.retaildemo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class StockEntry implements Serializable {
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

    public StockEntry() {
        this("", 0, 0, 0);
    }
}
