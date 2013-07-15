(ns store-server.web
  (:use compojure.core
        ring.middleware.json
        ring.util.response)
  (:require [store-server.catalogs.local :as catalog]))

(defn simple-logging-middleware [appw]
  (fn [req]
    (println req)
    (appw req)))

(defroutes handler
  (GET "/" []
    (response {"hello" "world"}))

  (PUT "/" [name]
    (response {"hello" name}))

  (POST "/scans" [code]
    (response (catalog/get-cpg (keyword code))))

  (POST "/scale-scans" [code]
    (response (catalog/get-bulk (keyword code)))))

(def app
  (-> handler
    wrap-json-params
    wrap-json-response
    simple-logging-middleware))
