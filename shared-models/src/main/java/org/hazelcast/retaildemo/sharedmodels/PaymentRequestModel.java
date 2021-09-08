package org.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class PaymentRequestModel {

    Long orderId;
    List<OrderLineModel> orderLines;

    public PaymentRequestModel() {
        this(0L, List.of());
    }

}
