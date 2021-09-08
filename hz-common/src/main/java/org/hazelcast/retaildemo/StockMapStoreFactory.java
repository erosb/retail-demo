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

public class StockMapStoreFactory
        implements MapStoreFactory<String, StockEntry> {

    public static class StockMapStore
            extends MapStoreAdapter<String, StockEntry> {

        private static final String STORE_SQL_STMT =
                "INSERT INTO stock (product_id, available_quantity, reserved_quantity) "
                        + "VALUES (?, ?, ?) "
                        + "ON CONFLICT (product_id) DO UPDATE SET available_quantity = ?, reserved_quantity = ?";

        private static final String LOAD_SQL_STMT = "SELECT available_quantity, reserved_quantity "
                + "FROM stock WHERE product_id = ?";

        private final Connection conn;

        public StockMapStore(String connectionUrl, String username, String password) {
            try {
                conn = DriverManager.getConnection(connectionUrl, username, password);
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void delete(String key) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stock WHERE product_id = ?")) {
                ps.setString(1, key);
                ps.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void store(String key, StockEntry value) {
            try (PreparedStatement stmt = conn.prepareStatement(STORE_SQL_STMT)) {
                stmt.setString(1, key);
                stmt.setInt(2, value.getAvailableQuantity());
                stmt.setInt(3, value.getReservedQuantity());
                stmt.setInt(4, value.getAvailableQuantity());
                stmt.setInt(5, value.getReservedQuantity());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public StockEntry load(String key) {
            try (PreparedStatement loadStatement = conn.prepareStatement(LOAD_SQL_STMT)) {
                loadStatement.setString(1, key);
                ResultSet rs = loadStatement.executeQuery();
                if (rs.next()) {
                    return StockEntry.builder()
                            .productId(key)
                            .availableQuantity(rs.getInt("available_quantity"))
                            .reservedQuantity(rs.getInt("reserved_quantity"))
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
    public MapLoader<String, StockEntry> newMapStore(String mapName, Properties props) {
        return new StockMapStore(
                props.getProperty("connectionUrl"),
                props.getProperty("username"),
                props.getProperty("password")
        );
    }
}
