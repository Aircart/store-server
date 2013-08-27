(ns store-server.models.receipt
  (:require [leveldb-clj.core :as db]
            [store-server.models.card :as card]))

;;
;; store key: receipt_storeid_userid_datetime
;;       key: user_user_id_receipts
;; default store: lab
;; receipt information:
;; - store-id
;; - time
;; - cart-id   -- this still would not freeze item tax calculations
;; - total-map
;; - charge-id

;; create new receipt from cart and charge
(defn create [dbd user-id cart total-map charge-id]
  "Create a new receipt using the charge-id authorization and cart for user-id.
  Returns the receipt-id (and unlink the cart) or nil if the capture failed."
  (let [success? (card/capture charge-id)
    ;; get taxes  - how to ensure same as checkpoint?
    ;; get totals (compute from get taxes)
    ]))

;; list user receipts

;; get details of a user receipt

;; get store receipts

