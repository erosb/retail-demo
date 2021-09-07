package org.hazelcast.retaildemo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
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
}
