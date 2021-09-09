package org.hazelcast.retaildemo;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class ShippableOrder {
    Long orderId;
    AddressModel deliveryAddress;
    String transactionId;
    String invoiceDocUrl;
    List<ShippableOrderLine> orderLines;
}
