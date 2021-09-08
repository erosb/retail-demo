package org.hazelcast.retaildemo.stockservice;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.hazelcast.retaildemo.StockEntry;
import org.hazelcast.retaildemo.sharedmodels.OrderModel;
import org.hazelcast.retaildemo.sharedmodels.PaymentFinishedModel;
import org.hazelcast.retaildemo.sharedmodels.PaymentRequestModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootApplication
@Slf4j
public class StockService {

    public static void main(String[] args) {
        SpringApplication.run(StockService.class, args);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
    private KafkaTemplate<String, PaymentRequestModel> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockRepository stockRepository;

    private final HazelcastInstance hzClient = HazelcastClient.newHazelcastClient();

    @KafkaListener(topics = "new-orders", groupId = "test")
    public void newOrder(OrderModel order) {
        log.info("received new-orders: {}", order);
        IMap<String, StockEntry> stockMap = hzClient.getMap("stock");
        txTemplate.executeWithoutResult(status ->
                order.getOrderLines().forEach(line -> {
                    int requestedQuantity = line.getQuantity();
                    StockEntry stockEntry = stockMap.get(line.getProductId());
                    if (stockEntry == null) {
                        stockEntry = stockRepository.findById(line.getProductId()).orElseThrow(IllegalArgumentException::new);
                    }
                    if (stockEntry.getAvailableQuantity() < requestedQuantity) {
                        // error handling
                        return;
                    }
                    stockEntry.incReserved(requestedQuantity);
                    stockEntry.decAvailable(requestedQuantity);
                    stockMap.put(line.getProductId(), stockEntry);
                    jdbcTemplate.update("UPDATE stock SET "
                                    + "reserved_quantity = reserved_quantity + ?, "
                                    + "available_quantity = available_quantity - ? "
                                    + "WHERE product_id = ?",
                            line.getQuantity(),
                            line.getQuantity(),
                            line.getProductId());

//                    int requestedQuantity = line.getQuantity();
//                    StockEntry stockEntry = stockMap.get(line.getProductId());
//                    if (stockEntry == null) {
//                        stockEntry = stockRepository.findById(line.getProductId()).orElseThrow(IllegalArgumentException::new);
//                        stockMap.put(line.getProductId(), stockEntry);
//                    }
//                    boolean isAvailable = stockMap.executeOnKey(line.getProductId(), new ReservationEntryProcessor(requestedQuantity));
//                    if (isAvailable) {
//                        jdbcTemplate.update("UPDATE stock SET "
//                                        + "reserved_quantity = reserved_quantity + ?, "
//                                        + "available_quantity = available_quantity - ? "
//                                        + "WHERE product_id = ?",
//                                line.getQuantity(),
//                                line.getQuantity(),
//                                line.getProductId());
//                    }
                }));
        var orderId = orderRepository.save(order);
        kafkaTemplate.send("payment-request", PaymentRequestModel.builder()
                .orderId(orderId)
                .orderLines(order.getOrderLines())
                .build());
    }

    @KafkaListener(topics = "payment-finished", groupId = "test")
    public void paymentFinished(PaymentFinishedModel paymentFinished) {
        log.info("received paymentFinished: {}", paymentFinished);
        Long orderId = paymentFinished.getOrderId();
        var orderLines = orderRepository.findOrderLinesByOrderId(orderId);

        txTemplate.executeWithoutResult(status -> {
            orderLines.forEach(line -> {
                IMap<String, StockEntry> stockMap = hzClient.getMap("stock");
                stockMap.executeOnKey(line.getProductId(), new PaymentFinishedEntryProcessor(paymentFinished.isSuccess(),
                        line.getQuantity()));
                jdbcTemplate.update("UPDATE stock SET reserved_quantity = reserved_quantity - ? "
                                + "WHERE product_id = ?",
                        line.getQuantity(),
                        line.getProductId());
                if (!paymentFinished.isSuccess()) {
                    jdbcTemplate.update("UPDATE stock SET available_quantity = available_quantity + ? "
                                    + "WHERE product_id = ?",
                            line.getQuantity(),
                            line.getProductId());
                }
            });
            status.flush();
        });
    }
}
