package org.hazelcast.retaildemo.stockservice;

import com.hazelcast.map.EntryProcessor;
import lombok.RequiredArgsConstructor;
import org.hazelcast.retaildemo.StockEntry;

import java.util.Map;

@RequiredArgsConstructor
public class PaymentFinishedEntryProcessor implements EntryProcessor<String, StockEntry, Void> {

    private final boolean isSuccess;
    private final int quantity;

    @Override
    public Void process(Map.Entry<String, StockEntry> mapEntry) {
        StockEntry stockEntry = mapEntry.getValue();
        stockEntry.decReserved(quantity);
        if (!isSuccess) {
            stockEntry.incAvailable(quantity);
        }
        mapEntry.setValue(stockEntry);
        System.out.println("processed paymentFinished at " + Thread.currentThread().getName());
        return null;
    }
}
