(ns store-server.models.cart
  (:use store-server.util)
  (:require [leveldb-clj.core :as db]
            [store-server.catalogs.local :as catalog]))

;; Cart Model
;;
;; store key: cart_userid_storeid_token
;;       key: user_userid_cart
;;
;; cart struct: code => quantity
;; {
;;   "3000" 30.3
;;   "5901234123457" 2
;; }
;;

(def tax_rate 5.75)

(defn create [db-descriptor user-id] ;; should the db operations be batched?
  "create a new cart and assigns it to the user default cart,
  store_id is not supported/stored yet, defaulting to 'lab'."
  ;; SECURITY: none for duplicate random-string
  (let [cart-id (str "cart_" user-id "_lab_" (random-string 8))]
    ;; store empty cart
    (db/put db-descriptor (.getBytes cart-id)
                          (.getBytes (pr-str {})))
    ;; save it as current cart
    (db/put db-descriptor (.getBytes (str "user_" user-id "_cart"))
                          (.getBytes cart-id))))

;; how to get correct order for phone?/switch for checkpoint
(defn fetch [db-descriptor user-id]
  "get current cart for a given user"
  (let [cart-id-bytes (db/get db-descriptor (.getBytes (str "user_" user-id "_cart")))]
    (if-not (nil? cart-id-bytes)
      (read-string (String. (db/get db-descriptor cart-id-bytes))))))

(defn change [db-descriptor user-id code & {:keys [qt increment?] :or {qt 1 increment? true}}]
  "Add an item or update the quantity of an existant item,
  if the quantity is 0, remove the item.
  Returns true if the item existed, false otherwise." ; update to new quantity if existed, false otherwise?
  ;; TODO: make transactional
  (let [cart-id-bytes (db/get db-descriptor (.getBytes (str "user_" user-id "_cart")))]
    (if-not (nil? cart-id-bytes)
      (do
        (db/put db-descriptor cart-id-bytes
          (.getBytes (pr-str
            (let [cart (read-string (String. (db/get db-descriptor cart-id-bytes)))]
              (if (= 0 qt)
                (dissoc cart code) ; in this case, existed doesn't get defined, does it crash?
                (let [item (code cart)]
                  (def existed? (not (nil? item))) ; NOTE: is there a more Clojure idiomatic way to do this?
                  (if (or (nil? item) (not increment?))
                    (assoc cart code qt)
                    (update-in cart [code] + qt))))))))
        existed?))))

(defn compute-total [cart] ; TODO: store prices on cart (expires?), hence avoid catalog access
  "Compute the cart subtotal, and total with taxes."
  (let [subtotal (with-precision 2 (/
    (apply + (for [[k v] cart]
      (*
        v
        (if (< 5 (.length (name k))) ; test wether plu or barcode
          ((catalog/get-cpg (keyword k)) :price)
          ((catalog/get-bulk (keyword k)) :price_per_gram)))))
    100M))]
    { :sub_total subtotal
      :total    (with-precision 2 (* subtotal (+ (/ tax_rate 100) 1)))}))
