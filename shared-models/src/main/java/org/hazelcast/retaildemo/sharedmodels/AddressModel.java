package org.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class AddressModel implements Serializable {
    Long id;
    String country;
    String postalCode;
    String city;
    String streetAddress;
    public AddressModel() {
        this(null, null, null, null, null);
    }
}
