package com.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PaymentFinishedModel {

    boolean isSuccess;
    OrderModel order;

    public PaymentFinishedModel() {
        this(false, OrderModel.builder().build());
    }
}
