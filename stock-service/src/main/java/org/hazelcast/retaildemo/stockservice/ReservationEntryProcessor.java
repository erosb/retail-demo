package org.hazelcast.retaildemo.stockservice;

import com.hazelcast.map.EntryProcessor;
import lombok.RequiredArgsConstructor;
import org.hazelcast.retaildemo.StockEntry;

import java.util.Map;

@RequiredArgsConstructor
public class ReservationEntryProcessor implements EntryProcessor<String, StockEntry, Boolean> {

    private final int requestedQuantity;

    @Override
    public Boolean process(Map.Entry<String, StockEntry> mapEntry) {
        StockEntry entry = mapEntry.getValue();
        if (entry.getAvailableQuantity() < requestedQuantity) {
            return false;
        }
        entry.incReserved(requestedQuantity);
        entry.decAvailable(requestedQuantity);
        mapEntry.setValue(entry);
        return true;
    }
}
