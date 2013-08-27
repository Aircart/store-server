(ns store-server.controllers.carts
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
(defn add-cpg-item [barcode user-id dbd]
  (if-let [cart-id-bytes (cart/get-cart-id-bytes dbd user-id)]
    ;; TODO: - add method to fetch item without dealing with store ns
    ;;       - separate logic so as not to deal with cart-id-bytes
    (let [cart (cart/fetch-with-cib dbd cart-id-bytes) store-space (symbol ((store/fetch (cart :store)) :ns))]
      (if-let [item-details ((ns-resolve store-space 'get-cpg) barcode)]
        (if (pos? (cart/update-and-write dbd cart cart-id-bytes barcode :price (item-details :price)))
          { :status 204 }
          { :status 201
            :headers { "Location" (str "/cart-items/" barcode) }
            :body    item-details })
        { :status 404 }))
    { :status 403 }))

(defn add-bulk-item [plu weight user-id dbd]
  (if-let [cart-id-bytes (cart/get-cart-id-bytes dbd user-id)]
    ;; TODO: - add method to fetch item without dealing with store ns
    ;;       - separate logic so as not to deal with cart-id-bytes
    (let [cart (cart/fetch-with-cib dbd cart-id-bytes) store-space (symbol ((store/fetch (cart :store)) :ns))]
      (if-let [item-details ((ns-resolve store-space 'get-bulk) plu)]
        (if (pos? (cart/update-and-write dbd cart cart-id-bytes plu :qt weight :price (item-details :price_per_gram)))
          { :status 204 }
          { :status 201
            :headers { "Location" (str "/cart-items/" plu) }
            :body    item-details })
        { :status 404 }))
    { :status 403 }))

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
