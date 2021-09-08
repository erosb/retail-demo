package org.hazelcast.retaildemo.stockservice;

import com.hazelcast.map.EntryProcessor;
import lombok.RequiredArgsConstructor;
import org.hazelcast.retaildemo.StockEntry;

import java.util.Map;

@RequiredArgsConstructor
public class PaymentFinishedEntryProcessor implements EntryProcessor<String, StockEntry, Void> {

    private final boolean isSuccess;
    private final String productId;
    private final int quantity;

    @Override
    public Void process(Map.Entry<String, StockEntry> mapEntry) {
        StockEntry stockEntry = mapEntry.getValue();
        System.out.println("START PaymentFinished " + stockEntry.getProductId() + " at " + Thread.currentThread().getName());
        stockEntry.decReserved(quantity);
        if (!isSuccess) {
            System.out.println("END-EARLY PaymentFinished " + stockEntry.getProductId() + " at " + Thread.currentThread().getName());
            stockEntry.incAvailable(quantity);
        }
        mapEntry.setValue(stockEntry);
        System.out.println("END PaymentFinished " + stockEntry.getProductId() + " at " + Thread.currentThread().getName());
        return null;
    }
}
