package org.hazelcast.retaildemo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Properties;

@RequiredArgsConstructor
public class JetJobSubmitter
        implements Runnable {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static PaymentFinishedModel jsonToDomainObj(ObjectNode on) {
        try {
            return OBJECT_MAPPER.treeToValue(on, PaymentFinishedModel.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private final HazelcastInstance hzClient;
    private final Properties kafkaProps;

    @Override
    public void run() {

        ServiceFactory<?, OrderLineReader> serviceFactory = ServiceFactories.sharedService(
                ctx -> new OrderLineReader());
        StreamSource<Map.Entry<String, ObjectNode>> source = KafkaSources.kafka(kafkaProps, "payment-finished");
        Pipeline pipeline = Pipeline.create();
        pipeline.readFrom(source).withoutTimestamps()
//                .map(entry -> dummy(entry.getValue()))
                .map(entry -> jsonToDomainObj(entry.getValue()))
//                .map(dummy -> Util.entry(dummy.getKey(), jsonToDomainObj(dummy.getValue())))

                .peek()
                .map(paymentFinished -> Util.entry(paymentFinished.getOrderId(), paymentFinished))

//                                                .filter(entry -> entry.getValue().isSuccess())
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

    private Object dummy(ObjectNode value) {
        return value;
    }

}
