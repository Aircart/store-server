(ns store-server.web
  (:use compojure.core
        ring.middleware.json
        ring.util.response)
  (:require [store-server.catalog :as catalog]))

(defroutes handler
  (GET "/" []
    (response {"hello" "world"}))

  (PUT "/" [name]
    (response {"hello" name}))

  (PUT "/scans" [code]
    (response (catalog/get (keyword code)))))

(def app
  (-> handler
    wrap-json-params
    wrap-json-response))
