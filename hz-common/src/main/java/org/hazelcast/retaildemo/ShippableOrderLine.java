package org.hazelcast.retaildemo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ShippableOrderLine {

    String productId;
    String productDescription;
    int quantity;
    int unitPrice;
    int totalPrice;

}
