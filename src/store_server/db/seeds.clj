(ns store-server.db.seeds
  (:require [clj-yaml.core :as yaml]
            [store-server.models.user :as user]
            [store-server.controllers.carts :as carts]
            [store-server.models.cart :as cart]
            [store-server.models.card :as card]))

(defn seed-cart [cart-name user-id dbd]
  ;; test user exists
  (when (user/fetch dbd user-id)
    (let [seed-data (yaml/parse-string (slurp "db/seeds.yaml"))]
      ;; test cart-name exists
      (when-let [cart-data (seed-data (keyword cart-name))]
        ;; create a new cart
        (carts/create-cart "aircart_lab" user-id dbd)
        ;; scan items, TODO: interract only with carts controller methods
        (doseq [[code qt] cart-data]
          (carts/add-item code qt user-id dbd))))))

(defn link-payment-account [stripe-id user-id dbd]  
  (card/link-account dbd user-id stripe-id))

; (defn seed-receipts []
;   )
