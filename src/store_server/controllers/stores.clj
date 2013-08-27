(ns store-server.controllers.stores
  (:require [store-server.models.store :as store]))

(defn get-store [store-id]
  (if-let [store-map (store/fetch store-id)]
    { :status 200
      :body   (select-keys store-map [:name :tax_rate])}
    { :status 404 }))
