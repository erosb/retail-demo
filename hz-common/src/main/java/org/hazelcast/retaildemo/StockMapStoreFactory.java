package org.hazelcast.retaildemo;

import com.hazelcast.map.MapLoader;
import com.hazelcast.map.MapStoreFactory;

import java.util.Properties;

public class StockMapStoreFactory implements MapStoreFactory<String, StockEntry> {
    @Override
    public MapLoader<String, StockEntry> newMapStore(String mapName, Properties properties) {
        return new StockMapStore(properties);
    }
}
