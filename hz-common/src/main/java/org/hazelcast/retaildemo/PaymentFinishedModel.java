package org.hazelcast.retaildemo;

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
    String transactionId;
    String invoiceDocUrl;

    public PaymentFinishedModel() {
        this(false, 0L, null, null);
    }
}
