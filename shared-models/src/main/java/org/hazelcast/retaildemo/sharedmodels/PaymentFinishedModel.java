package org.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class PaymentFinishedModel implements Serializable {

    boolean isSuccess;
    Long orderId;

    public PaymentFinishedModel() {
        this(false, 0L);
    }
}
