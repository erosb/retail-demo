package org.hazelcast.retaildemo;

import com.hazelcast.map.MapStoreAdapter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class StockMapStore
        extends MapStoreAdapter<String, StockEntry> {

    public static final String STORE_SQL_STMT =
            "INSERT INTO stock (product_id, available_quantity, reserved_quantity, unit_price) "
                    + "VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT (product_id) DO UPDATE SET available_quantity = ?, reserved_quantity = ?, unit_price = ?";
    private final Connection conn;

    private final ConcurrentHashMap<String, StockEntry> store = new ConcurrentHashMap<>();

    public StockMapStore(Properties props) {
        try {
            conn = DriverManager.getConnection(props.getProperty("connectionUrl"),
                    props.getProperty("username"), props.getProperty("password"));
            conn.setAutoCommit(true);
//            storeStatement = conn.prepareStatement(STORE_SQL_STMT);
//            loadStatement = conn.prepareStatement(
//                    ("SELECT * FROM stock WHERE product_id = ?", "SELECT available_quantity, reserved_quantity, unit_price"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM stock WHERE product_id = ?");
            ps.setString(1, key);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConn() {
        try {
            return DriverManager.getConnection("jdbc:postgresql://db:5432/hz-demo",
            "postgres", "postgres");
        } catch (SQLException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void store(String key, StockEntry value) {
//        log("begin store %s", key);
//        store.put(key, value);
        try (Connection conn = getConn(); PreparedStatement stmt = conn.prepareStatement(
        //        STORE_SQL_STMT
                "UPDATE stock SET available_quantity = ?, reserved_quantity = ?, unit_price = ? WHERE product_id = ?"
        )){
            stmt.closeOnCompletion();
            stmt.setInt(1, value.getAvailableQuantity());
            stmt.setInt(2, value.getReservedQuantity());
            stmt.setInt(3, value.getUnitPrice());
            stmt.setString(4, key);
//            stmt.setInt(5, value.getAvailableQuantity());
//            stmt.setInt(6, value.getReservedQuantity());
//            stmt.setInt(7, value.getUnitPrice());
            log("start execute... %s", key);
            stmt.executeUpdate();
            log("finish execute... %s", key);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            log("------------");
        }
    }

    private void log(String msg, Object ...args) {
        System.out.println(String.format(msg, args) + " {" + Thread.currentThread().getName() + "}");
    }

    @Override
    public StockEntry load(String key) {
        return store.get(key);
//        try (PrepPsta
//        ){
//            loadStatement.setString(1, key);
//            ResultSet rs = loadStatement.executeQuery();
//            if (rs.next()) {
//                return StockEntry.builder()
//                        .productId(key)
//                        .availableQuantity(rs.getInt("available_quantity"))
//                        .reservedQuantity(rs.getInt("reserved_quantity"))
//                        .unitPrice(rs.getInt("unit_price"))
//                        .build();
//            }
//            rs.close();
//        } catch (SQLException e) {
//            throw new RuntimeException();
//        }
    }
}
