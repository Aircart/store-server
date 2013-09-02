(ns store-server.controllers.carts
  (:use store-server.util)
  (:require [store-server.models.cart :as cart]
            [store-server.models.store :as store]
            [store-server.catalogs.local :as local]
            [store-server.catalogs.factual :as factual]))

(defn create-cart [store-id user-id dbd]
  (if (cart/create dbd user-id store-id)
    { :status 201 }
    { :status 404 }))

(defn get-current-cart [user-id dbd]
  (if-let [cart (cart/fetch dbd user-id)]
    { :status 200
      :body {
        :store_id (cart :store)
        :items    (cart/attach-item-details (cart :store) (cart :items))}}
    { :status 404 }))

;; separate controller for scans?
;; allow scanning item with given quantity?
(defn add-item [code qt user-id dbd]
  (if-let [cart-id-bytes (cart/get-cart-id-bytes dbd user-id)]
    ;; TODO: - add method to fetch item without dealing with store ns
    ;;       - separate logic so as not to deal with cart-id-bytes
    (let [cart (cart/fetch-with-cib dbd cart-id-bytes) store-space (symbol ((store/fetch (cart :store)) :ns))]
      (if-let [item-details ((ns-resolve store-space (if (plu? code) 'get-bulk 'get-cpg)) code)]
        (if (pos? (cart/update-and-write dbd cart cart-id-bytes code :qt qt :price (item-details (if (plu? code) :price_per_gram :price))))
          { :status 204 }
          { :status 201
            :headers { "Location" (str "/cart-items/" code) }
            :body    item-details })
        { :status 404 }))
    { :status 403 }))

(defn add-cpg-item [barcode user-id dbd]
  (add-item barcode 1 user-id dbd))

(defn add-bulk-item [plu weight user-id dbd]
  (add-item plu weight user-id dbd))

(defn change-item-quantity [code quantity user-id dbd]
  (if-let [cart-id-bytes (cart/get-cart-id-bytes dbd user-id)]
    ;; TODO: - add method to fetch item without dealing with store ns
    ;;       - separate logic so as not to deal with cart-id-bytes
    (let [cart (cart/fetch-with-cib dbd cart-id-bytes) kcode (keyword code)]
      (if (cart :items kcode)
        (do
          (cart/update-and-write dbd cart cart-id-bytes code :qt quantity :reset? true)
          { :status 204 })
        { :status 404 }))
    { :status 403 }))

(defn remove-item [code user-id dbd]
  (change-item-quantity code 0 user-id dbd))
