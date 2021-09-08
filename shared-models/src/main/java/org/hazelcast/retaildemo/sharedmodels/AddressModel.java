package org.hazelcast.retaildemo.sharedmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AddressModel {
    Long id;
    String country;
    String postalCode;
    String city;
    String streetAddress;
    public AddressModel() {
        this(null, null, null, null, null);
    }
}
