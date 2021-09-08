package org.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Builder
@AllArgsConstructor
public class OrderModel {

    Long orderId;

    List<OrderLineModel> orderLines;

    AddressModel shippingAddress;

    AddressModel invoiceAddress;

    public OrderModel() {
        this(null, emptyList(), null, null);
    }
}
