package org.hazelcast.retaildemo;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class ShippableOrder implements Serializable {
    Long orderId;
    AddressModel shippingAddress;
    String transactionId;
    String invoiceDocUrl;
    List<ShippableOrderLine> orderLines;
}
