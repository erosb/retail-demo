package org.hazelcast.retaildemo;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder(toBuilder = true)
public class ShippableOrderLine implements Serializable {

    String productId;
    String productDescription;
    int quantity;
    int unitPrice;
    int totalPrice;

}
