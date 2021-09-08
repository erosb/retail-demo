package org.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class OrderLineModel {
    String productId;
    int quantity;

    public OrderLineModel() {
        this("", 0);
    }

}
