# store-server

This is the main program that runs the prototype store. It is a single jar that handles both HTTP and Websocket APIs. Per our architecture design, this applicaiton handles both load balancing and higher level database functionnality internally.

## Prerequisites

Java 7, block-level disk access.

## Devices API Documentation

For API doc, go to: <http://docs.aircart.apiary.io/>

## Seeding Web Commands

```
GET /seeding/cart/[user-id]
```
This will seed a cart with random items from `aircart_lab`.

```
GET /seeding/cards/[user-id]/[stripe-id]
```
This will give this user all the credit cards associated with the given Stripe customer ID.


## Image Processing

To process images, an image proxy should be used: <http://images.weserv.nl/>

We will have our own setup in the future.
