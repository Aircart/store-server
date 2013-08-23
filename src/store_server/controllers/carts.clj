(ns store-server.controllers.carts
  (:require [store-server.models.cart :as cart]
            [store-server.catalogs.local :as catalog]))

(defn create-cart [user-id dbd]
  (cart/create dbd user-id)
  { :status 201
    :body { :tax_rate cart/tax_rate }})

(defn get-current-cart [user-id dbd]
  (let [cart (cart/fetch dbd user-id)]
    (if (nil? cart)
      { :status 404 }
      { :status 200
        :body {
          :store_id "aircart_lab"
          :tax_rate cart/tax_rate ;; ISSUE: name conflict with let?
          :items    (vec (for [[k v] cart]
                      (if (< 5 (.length (name k))) ; test wether plu or barcode
                        (merge { :barcode k
                                 :quantity v }
                               (catalog/get-cpg (keyword k)))
                        (merge { :plu k
                                 :weight v }
                               (catalog/get-bulk (keyword k))))))}})))

;; separate controller for scans?
(defn add-cpg-item [barcode user-id dbd]
  (let [code (keyword barcode) existed? (cart/change dbd user-id code)]
    (if-not (nil? existed?) ; TODO: add error code for scan with no cart
      (if existed?
        { :status 204 }
        { :status 201
          :headers { "Location" (str "/cart-items/" barcode) }
          :body (catalog/get-cpg code) }))))

(defn add-bulk-item [plu weight user-id dbd]
  (let [code (keyword plu) existed? (cart/change dbd user-id code :qt weight)]
    (if-not (nil? existed?) ; TODO: add error code for scan with no cart
      (if existed?
        { :status 204 }
        { :status 201
          :headers { "Location" (str "/cart-items/" plu) }
          :body (catalog/get-bulk code) }))))

(defn change-item-quantity [code quantity user-id dbd]
  ;; this will create an item if none was scanned before...
  (if-not (nil? (cart/change dbd user-id (keyword code) :qt quantity :increment? false))
    { :status 204 }))

(defn remove-item [code user-id dbd]
  (if-not (nil? (cart/change dbd user-id (keyword code) :qt 0))
    { :status 204 }))
