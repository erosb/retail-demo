package com.hazelcast.retaildemo.stockservice;

import com.hazelcast.retaildemo.sharedmodels.OrderModel;
import com.hazelcast.retaildemo.sharedmodels.PaymentFinishedModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootApplication
@Slf4j
public class StockServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockServiceApplication.class, args);
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TransactionTemplate txTemplate;

	@KafkaListener(topics = "new-orders", groupId = "test")
	public void newOrder(OrderModel order) {
		txTemplate.execute(status -> {
			order.getOrderLines().forEach(line -> {
				jdbcTemplate.update("UPDATE stock SET reserved = reserved + ?, availableQuantity = availableQuantity - ? "
								+ " WHERE productId = ?",
						line.getQuantity(),
						line.getQuantity(),
						line.getProductId());
			});
			return null;
		});
	}

	@KafkaListener(topics = "payment-finished", groupId = "test")
	public void paymentFinished(PaymentFinishedModel paymentFinished) {
		log.info("received paymentFinished: {}", paymentFinished);
		txTemplate.execute(status -> {
			return null;
		});
	}
}
