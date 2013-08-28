(ns store-server.models.cart
  (:use store-server.util)
  (:require [leveldb-clj.core :as db]
            [store-server.models.store :as store]
            [store-server.catalogs.local :as local]
            [store-server.catalogs.factual :as factual]))

;; TODO: use a vector or maps vs. array-map
;;       - this will help with using ProtoBuf
;;       - this will allow using strings vs keywords for codes

;; Cart Model
;;
;; store key: cart_userid_storeid_token
;;       key: user_userid_cart
;;
;; cart struct:
;; {
;;   :store "store_id"
;;   :items
;;   {
;;     :3000          { :price 3.2 :qt 30.3 }
;;     :5901234123457 { :price 4.5 :qt 2    }
;;   }
;; }

(defn user-to-cib [user-id]
  (.getBytes (str "user_" user-id "_cart")))

(defn get-cart-id-bytes [dbd user-id]
  (db/get dbd (user-to-cib user-id)))

; TODO: expire cart? (price change issues)
(defn create [db-descriptor user-id store-id] ;; should the db operations be batched?
  "Create a new cart and assigns it to the user default cart.
  Returns the created cart ID or nil if the store did not exist."
  ;; SECURITY: none for duplicate random-string
  ;; TODO: use WriteBatch
  (when (store/fetch store-id)
    (doto
      (str "cart_" user-id "_" store-id "_" (random-string 8))
      (#(do
         ;; store empty cart
         (db/put db-descriptor (.getBytes %)
                               (.getBytes (pr-str { :store store-id :items (array-map) })))
         ;; save it as current cart
         (db/put db-descriptor (user-to-cib user-id)
                               (.getBytes %)))))))

(defn fetch-with-cib [dbd cart-id-bytes]
  (read-string (String. (db/get dbd cart-id-bytes))))

;; how to get correct order for phone?/switch for checkpoint
(defn fetch [db-descriptor user-id]
  "Get current cart for a given user"
  (when-let [cart-id-bytes (get-cart-id-bytes db-descriptor user-id)]
    (fetch-with-cib db-descriptor cart-id-bytes)))

;; TODO: remove concern of bytes for callers
(defn update-and-write [dbd cart cart-id-bytes code & {:keys [qt reset? price] :or {qt 1 reset? false}}]
  "Add an item, remove it, or update the quantity of an existing item.
  Returns the previous quantity.
  * Cart existence must be checked beforehand.
  * Price must be provided if it's a new item or it will be nil."
  ;; TODO: make transactional
  (let [kcode (keyword code)]
    (doto
      (or (-> cart :items kcode :qt) 0) ; % previous quantity
      (#(when (if reset? (not= % qt) (not= 0 qt)) ; ensure there is something to change
        (db/put dbd cart-id-bytes
          (.getBytes (pr-str
            (if (= 0 qt)
              (dissoc cart [:items kcode])
              ;; TODO: handle item not found? -> done by controller
              (assoc-in cart [:items kcode] { :price (if (= 0 %)
                                                price
                                                (-> cart :items kcode :price))
                                              :qt (if reset? qt (+ % qt)) }))))))))))

(defn compute-total [cart]
  "Compute the cart subtotal, and total with taxes."
  (let [subtotal (with-precision 2 (/   ; convert from cents to decimal dollars
                   (apply + (for [[k v] (cart :items)] ; reduce the subtotal of each item
                     (* (v :price) (v :qt))))          ; 
                   100M))
        tax-rate ((store/fetch (cart :store)) :tax_rate)]
    { :subtotal subtotal
      :tax_rate tax-rate
      :total    (with-precision 2 (* subtotal (+ (/ tax-rate 100) 1)))}))

(defn unlink-current-cart [dbd user-id]
  "Removes the user's current cart."
  )

(defn attach-item-details [store-id items]
  "Take items as stored in the cart struct and attach information from the 
  catalog corresponding to the store-id. Uses cpg/bulk formatting."
  (let [space (symbol ((store/fetch store-id) :ns))]
    (vec (for [[k v] items]
      (if (< 5 (.length (name k))) ; test wether plu or barcode
        (merge ((ns-resolve space 'get-cpg) k)
               { :barcode        (name k)
                 :quantity       (v :qt)
                 :price          (v :price) })
        (merge ((ns-resolve space 'get-bulk) k)
               { :plu            (name k)
                 :weight         (v :qt)
                 :price_per_gram (v :price) }))))))

(defn select-purchases [cart]
  "Format a cart for display at the checkpoint."
  (map #(dissoc % :price :price_per_gram)
    (attach-item-details (cart :store) (cart :items))))
