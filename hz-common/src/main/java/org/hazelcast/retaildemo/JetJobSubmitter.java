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
            throw new RuntimeException(e);
        }
    }

    private final HazelcastInstance hzClient;
    private final Properties kafkaProps;

    @Override
    public void run() {
        ServiceFactory<?, OrderLineDatabaseReader> serviceFactory = ServiceFactories.sharedService(
                ctx -> new OrderLineDatabaseReader());
        StreamSource<Map.Entry<String, ObjectNode>> source = KafkaSources.kafka(kafkaProps, "payment-finished");
        Pipeline pipeline = Pipeline.create();
        pipeline.readFrom(source).withIngestionTimestamps()
                .map(entry -> jsonToDomainObj(entry.getValue()))
                .filter(PaymentFinishedModel::isSuccess)
                .mapUsingService(serviceFactory, (orderLineDatabaseReader, paymentFinished) ->
                        ShippableOrder.builder()
                                .orderId(paymentFinished.getOrderId())
                                .transactionId(paymentFinished.getTransactionId())
                                .invoiceDocUrl(paymentFinished.getInvoiceDocUrl())
                                .orderLines(orderLineDatabaseReader.findOrderLinesByOrderId(paymentFinished.getOrderId()))
                                .build()
                )
                .<Long, AddressModel, ShippableOrder>mapUsingIMap("shipping_addresses",
                        order -> order.getOrderId(),
                        (order, address) -> order.toBuilder()
                                .shippingAddress(address)
                                .build()
                )
                .map(shippableOrder -> Util.entry(shippableOrder.getOrderId(), shippableOrder))
                .peek()
                .writeTo(Sinks.map("shippable_orders"));
        hzClient.getJet().newJob(pipeline);
    }
}
