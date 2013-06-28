# store-server

FIXME: description

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar store-server-0.1.0-standalone.jar [args]

## Amazon test instance

    http://

## Basic API Usage

Note: prices are in cents

Use header: `Content-Type: application/json`

    POST /scans
    {
      "code": "078742115238"
    }
    ->
    {
      "name": "Pasta"
      "image": "http://example.com/image.jpg"
      "price": 500
    }

    POST /scale-scans
    {
      "code": "3000"
    }
    ->
    {
      "name": "Pasta"
      "image": "http://example.com/image.jpg"
      "price_per_gram": 0.1
    }

## Image Processing

To process images, use an image proxy, we will have our own setup soon, use this for now: <https://gist.github.com/carlo/5379498>

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
