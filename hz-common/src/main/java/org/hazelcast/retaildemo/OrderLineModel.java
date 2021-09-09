package org.hazelcast.retaildemo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class OrderLineModel implements Serializable {
    String productId;
    int quantity;

    public OrderLineModel() {
        this("", 0);
    }

}
