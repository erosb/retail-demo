package org.hazelcast.retaildemo;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class JetJobSubmitter
        implements Runnable {

    private final HazelcastInstance hzClient;
    private final Properties kafkaProps;

    @Override
    public void run() {

        ServiceFactory<?, OrderLineReader> serviceFactory = ServiceFactories.sharedService(
                ctx -> new OrderLineReader());
        StreamSource<Map.Entry<String, PaymentFinishedModel>> source = KafkaSources.kafka(kafkaProps, "payment-finished");

        Pipeline pipeline = Pipeline.create();
        pipeline.readFrom(source).withoutTimestamps()
//                .filter(entry -> entry.getValue().isSuccess())
//                .mapUsingService(serviceFactory, (orderLineReader, paymentFinishedEntry) -> {
//                    PaymentFinishedModel paymentFinishedEntryValue = paymentFinishedEntry.getValue();
//                    List<OrderLineModel> orderLines = orderLineReader.findOrderLinesByOrderId(
//                            paymentFinishedEntryValue.getOrderId());
//                    return ShippableOrder.builder()
//                            .invoiceDocUrl(paymentFinishedEntryValue.getInvoiceDocUrl())
//                            .orderLines(orderLines.stream()
//                                    .map(orderLine -> ShippableOrderLine.builder()
//                                            .productId(orderLine.getProductId())
//                                            .quantity(orderLine.getQuantity())
//                                            .build()
//                                    ).collect(toList()))
//                            .build();
//                })
//                //                .mapUsingIMap("products", entry -> entry.getKey(), (order, product) -> 3)
//                .map(shippableOrder -> Map.entry(shippableOrder.getOrderId(), shippableOrder))
                .writeTo(Sinks.map("shippable_orders"));
        hzClient.getJet().newJob(pipeline);
    }

}
