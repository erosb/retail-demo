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

stock: IMap
productId -> { availableQuantity, reservedQuantity, unitPrice }

 - naiv implementáció
 - Entry processor

## Biztos, hogy előrébb vagyunk?

near-cache bejön

## Bonyolítsuk:

Megbízhatatlan PaymentService: nem biztos, hogy minden megkezdett fizetés után kapunk üzenetet, hogy sikeres/sikertelen a fizetés. Ez azért gond, mert beragad az order map-be a rendelés, a stock-ban meg reserved marad a mennyiség. 

Megoldás:
 - eviction a map-en
 - MapEventListener: eviction eventnél karbantartjuk a táblát.

## Bonyolítsuk: SupplyService

beszállításokról kapunk információt, tehát a stock.availableQuantity módusul.

A SupplyService ugyanúgy a postgres-hez nyúl, updateli, aztán ezért a stock IMap-et is karban kell tartania
    -> duplikált kód a két service között

Megoldás:

az IMap-re felteszünk egy MapStore-t

Innentől nincs is szükség a service-eknek a DB-vel kommunikálni, Hazelcaston keresztül megy.

TODO valami statikus információ: MapLoader

## Bonyolítsuk: a webshopnak valahonnan tudnia kell a készlet-adatokat


a stock IMap-ra egy streamet teszünk, grpc-vel kommunikálja vissza az adatokat



ProcurementService: ha adott termékből túl sokat túl gyorsan vesznek, akkor ezt streamen keresztül észreveszi, és értesíti a beszerzési részleget

price-ok állítgatása: külső alkalmazással, amihez viszont nem tudunk hozzáférni, legacy app, postgres-be ír -> bejövő CDC stream alapján frissítjük a Stock map-et