package org.hazelcast.retaildemo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder
@AllArgsConstructor
public class Product implements Serializable {

    String productId;
    String description;
    int unitPrice;
}
