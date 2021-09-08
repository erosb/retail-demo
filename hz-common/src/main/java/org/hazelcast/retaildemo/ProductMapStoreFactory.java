package org.hazelcast.retaildemo;

import com.hazelcast.map.MapLoader;
import com.hazelcast.map.MapStoreAdapter;
import com.hazelcast.map.MapStoreFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class ProductMapStoreFactory implements MapStoreFactory<String, Product> {

    private static class ProductMapStore extends MapStoreAdapter<String, Product> {

        private static final String LOAD_SQL_STMT = "SELECT description, unit_price FROM products WHERE product_id = ?";

        private final Connection conn;

        public ProductMapStore(String connectionUrl, String username, String password) {
            try {
                conn = DriverManager.getConnection(connectionUrl, username, password);
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Product load(String key) {
            try (PreparedStatement loadStatement = conn.prepareStatement(LOAD_SQL_STMT)) {
                loadStatement.setString(1, key);
                ResultSet rs = loadStatement.executeQuery();
                if (rs.next()) {
                    return Product.builder()
                            .productId(key)
                            .description(rs.getString("description"))
                            .unitPrice(rs.getInt("unit_price"))
                            .build();
                }
                rs.close();
            } catch (SQLException e) {
                throw new RuntimeException();
            }
            return null;
        }
    }

    @Override
    public MapLoader<String, Product> newMapStore(String mapName, Properties props) {
        return new ProductMapStore(
                props.getProperty("connectionUrl"),
                props.getProperty("username"),
                props.getProperty("password")
        );
    }
}
