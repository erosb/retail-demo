# Retail demo

## Initial Condition

 - the webshop sends messages to StockService on the new-order topic
 - StockService:
    - increases the reserved number, decreases the available one
    - then forwards the Order to the PaymentService
 - PaymentService communicates the payment process asynchronously with the webshop
    - then a message on the payment-finished topic:
        * succ/fail
        * Order objektum

Problem:
 - there are too many places in the Order object, along with addresses
 - so put the Order in a postgres and the Payment service will get one
 PaymentRequest objects:
    - orderLines
    - customerId


We also want to store the stock in postgres:

order
-----
 - shippingAddress
 - invoiceAddress
 - customerContact
 - orderLines

address
-------
 - countryCode
 - postalCode
 - county
 - city
 - streetName
 - streetAddress

stock
-----
 - productCode
 - availableQuantity
 - reservedQuantity
 - unitPrice

Problem:
- availableQuantity and unitPrice must always be queried from DB -> can be slow
 -? DB writing can be slow regardless of indexing


## Cache the stock table

Use Hazelcast IMap

stock: IMap
productId -> { availableQuantity, reservedQuantity, unitPrice }

 - naive implementation
 - Entry processor

## Are you sure we're ahead?

near-cache comes in

## Complication:

Untrusted PaymentService: We may not receive a message after each payment initiated that the payment was successful / unsuccessful. This is a problem because the order gets stuck in the order map, the quantity remains in the stock.

Solution:
 - eviction on the map
 - MapEventListener: we maintain the table in case of an eviction event.

## Complex: SupplyService

we get information about deliveries, so stock.availableQuantity mode.

SupplyService reaches postgres in the same way, updates, and then needs to maintain stock IMap
    -> duplicate code between two services

Solution:

we put a MapStore on IMap

From here, there is no need for services to communicate with the DB, it goes through Hazelcast.

TODO some static information: MapLoader

## Complicate: the webshop needs to know the inventory data from somewhere


we put a stream on stock IMap, it communicates back with grpc

ProcurementService: if you buy too much of a product too fast, it notices it through a stream and notifies the purchasing department

set prices: with external application, which we can't access, legacy app, writes to postgres -> we update the Stock map based on the incoming CDC stream