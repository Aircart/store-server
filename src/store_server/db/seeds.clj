(ns store-server.db.seeds
  (:require [store-server.catalogs.local :as catalog]
            [store-server.controllers.carts :as carts]
            [store-server.models.card :as card]))

(defn seed-cart [user-id dbd]
  ;; create a new cart
  (carts/create-cart "aircart_lab" user-id dbd)
  ;; scan items
  (doseq [code ["044800001447" "044800001447" "041220796076"]]
    (carts/add-cpg-item code user-id dbd)))

(defn link-payment-account [stripe-id user-id dbd]  
  (card/link-account dbd user-id stripe-id))

(defn seed-receipts []
  )
