package com.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class PaymentRequestModel {

    int orderId;
    List<OrderLineModel> orderLines;

    public PaymentRequestModel() {
        this(0, List.of());
    }

}
