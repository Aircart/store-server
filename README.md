# store-server

This is the main program that runs the prototype store. It is a single jar that handles both HTTP and Websocket APIs. Per our architecture design, this applicaiton handles both load balancing and higher level database functionnality internally.

## Prerequisites

Java 7, block-level disk access.

## Devices API Documentation

For API doc, go to: <http://docs.aircart.apiary.io/>

## Seeding Web Commands

### Seed cart
```
curl http(s)://host:port/seeding/cart/[user-id]/[cart-name]
```
This will seed the given cart-name to user-id. Example:
```
curl http://tariks-macbook-air.local:8080/seeding/cart/1031864300/web
```
will give Joe the same cart as on Aircart's homepage.

### Seed payment cards
```
curl http(s)://host:port/seeding/cards/[user-id]/[stripe-id]
```
This will give this user all the credit cards associated with the given Stripe customer ID. Example:
```
curl http://tariks-macbook-air.local:8080/seeding/cards/1031864300/cus_2HF3vQgQKy5aIo
```
will (re)connect Joe's Stripe account to his Aircart account, he will hence see all his credit cards appear again, in case the Aircart database was reset.


## Image Processing

To process images, use our shinning image proxy: 

Basic syntax:
```
http(s)://host:port/(resize|crop)/[x]x[y]/[url]
```

For example:
```
http://tariks-macbook-air.local:8181/crop/100x100/http://i1.ytimg.com/vi/SMQK9_N0pks/maxresdefault.jpg
```

Documentation: <https://github.com/beetlebugorg/mod_dims/wiki/Webservice-API>

Using an external proxy should be tested to simulated loading images over 3G, like: <http://images.weserv.nl/>
