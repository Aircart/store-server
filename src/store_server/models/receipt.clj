(ns store-server.models.receipt
  (:require [leveldb-clj.core :as db]
            [store-server.models.card :as card]
            [store-server.models.cart :as cart]))

;;
;; store key: receipt_storeid_userid_datetime
;;       key: user_userid_receipts
;; default store: lab
;; receipt information:
;; - time
;; - cart-id   -- this still would not freeze item tax calculations (contains store-id)
;; - total-map
;; - charge-id

;; TODO: some sort of framework for key formats/leveldb operations?
;;       - eg: -key for bytes, -id for string

;; TODO: move to util
(defn iso-format [date]
  ;; TODO: replace 'Z' with X for timezone format after upgrade to Java7
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'") date))

;; create new receipt from cart and charge
(defn create [dbd user-id cart-id total-map charge-id]
  "Create a new receipt using the charge-id authorization and cart for user-id.
  Returns the receipt-id (and unlink the cart) or nil if the capture failed."
  (when-let [success? (card/capture charge-id)]
    (cart/unlink-if-current dbd user-id cart-id)
    (let [cart (cart/fetch-with-id dbd cart-id) store-id (cart :store) jtime (java.util.Date.)]
      (doto
        (str "receipt_" store-id "_" user-id "_" ) ;<< add date
        (#(do
          ;; store new receipt
          (db/put dbd (.getBytes %) (.getBytes (pr-str ; TODO: put in serialize method
            { :cart-id   cart-id ; TODO: standarize object and object-id
              :total-map total-map
              :charge-id charge-id
              :time      jtime })))
          ;; append to user's receipts
          (let [key-bytes (.getBytes (str "user_" user-id "_receipts")) ; make method for it (reused in fetch-user-receipts)
                receipts  (or (-> (db/get dbd key-bytes) String. read-string) [])]
            (db/put dbd key-bytes (-> (conj receipts %) pr-str .getBytes)))))))))

;; list user receipts
(defn fetch-user-receipts [dbd user-id]
  "Fetch the receipts for a given user and add details for the API."
  (if-let [receipts (-> (db/get dbd (.getBytes (str "user_" user-id "_receipts"))) String. read-string)]
    (vec (for [receipt-id receipts]
      (let [receipt (-> (db/get dbd (.getBytes receipt-id)) String. read-string)
            cart    (cart/fetch-with-id (:cart-id receipt))]
        (merge
          { :id         receipt-id
            :time       (iso-format (:time receipt))
            :store_id   (:store cart)
            :item_count (count (:items cart)) }
          (:total-map receipt)))))
    []))

;; get details of a user receipt
;; SECURITY: ensure owned by user
(defn fetch-receipt-details [dbd receipt-id]
  "Fetch the details of a given receipt and format it for the API.
  Returns nil if the receipt does not exist."
  (when-let [receipt (-> (db/get dbd (.getBytes receipt-id)) String. read-string)]
    (let    [cart    (cart/fetch-with-id (:cart-id receipt))
             charge  (card/get-charge (:charge-id receipt))]
      (merge ; TODO: DRY up with above method
        { :id         receipt-id
          :time       (iso-format (:time receipt))
          :store_id   (:store cart)
          :item_count (count (:items cart)) }
          :cart_items (cart/attach-item-details (:store cart) (:items cart))
          :card       (select-keys (charge :card)
            [ :id
              :last4
              :type
              :exp_month
              :exp_year
              :name
              :address_zip ])
        (:total-map receipt)))))

;; get store receipts

