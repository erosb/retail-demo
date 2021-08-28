package com.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Builder
@AllArgsConstructor
public class OrderModel {


    List<OrderLineModel> orderLines;

    public OrderModel() {
        this(emptyList());
    }
}
