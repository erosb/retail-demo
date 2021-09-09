package org.hazelcast.retaildemo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class PaymentRequestModel implements Serializable {

    Long orderId;
    List<OrderLineModel> orderLines;

    public PaymentRequestModel() {
        this(0L, List.of());
    }

}
