package org.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PaymentFinishedModel {

    boolean isSuccess;
    Long orderId;

    public PaymentFinishedModel() {
        this(false, 0L);
    }
}
