package org.hazelcast.retaildemo.stockservice;

import org.hazelcast.retaildemo.sharedmodels.AddressModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends CrudRepository<AddressModel, Long> {


}
