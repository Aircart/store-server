# store-server

This is the main program that runs the prototype store. It is a single jar that handles both HTTP and Websocket APIs. Per our architecture design, this applicaiton handles both load balancing and higher level database functionnality internally.

## How to build

You need Java 7 and leiningen on your machine to make the jar.

```
lein uberjar
docker build -t aircart/store-server .
```

## How to run

Add `-p 8080:8080` if you want to test the api over an unsecure connection, otherwise use with [ssl-proxy](https://github.com/Aircart/ssl-proxy).

```
docker run --name api aircart/store-server
```

## Devices API Documentation

For API doc, go to: <http://docs.aircart.apiary.io/>

## Seeding Web Commands

### Seed cart
```
curl http(s)://host:port/seeding/cart/[user-id]/[cart-name]
```
This will seed the given cart-name to user-id. Example:
```
curl http://tariks-air:8080/seeding/cart/1031864300/web
```
will give Joe the same cart as on Aircart's homepage.

### Seed payment cards
```
curl http(s)://host:port/seeding/cards/[user-id]/[stripe-id]
```
This will give this user all the credit cards associated with the given Stripe customer ID. Example:
```
curl http://tariks-air:8080/seeding/cards/1031864300/cus_2HF3vQgQKy5aIo
```
will (re)connect Joe's Stripe account to his Aircart account, he will hence see all his credit cards appear again, in case the Aircart database was reset.
