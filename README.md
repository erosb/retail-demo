# Retail demo

## Kiinduló állapot

 - a webshop new-order topicon küld üzeneteket a StockService-nek
 - a StockService:
    - reserved számot növeli, available-t csökkenti
    - majd továbbküldi az Order-t a PaymentService-nek
 - a PaymentService aszinkron kommunikálja a webshoppal a fizetési folyamatot
    - majd üzenet a payment-finished topicra:
        * succ/fail
        * Order objektum

Problémák:
 - túl sok helyen ott az Order objektum, address-ekkel együtt
 - ezért tegyük be egy postgres-be az Order-t és a Payment service kapjon egy
 PaymentRequest objektumot:
    - orderLines
    - customerId

Szintén postgres-ben akarjuk tárolni a raktárkészletet:

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

Problémák:
 - az availableQuantity-t, meg a unitPrice-ot mindig DB-ből kell lekérdezni -> lassú lehet
 -? DB írás indexeléstől függetlenül lassú lehet


## Cache-eljük be a stock táblát

Használjunk Hazelcast IMap-et