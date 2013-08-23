(ns store-server.models.receipt
  (:require [leveldb-clj.core :as db]
            [store-server.models.card :as card]))

;;
;; store key: receipt_storeid_userid_datetime
;;       key: user_user_id_receipts
;; default store: lab

;; create new receipt from cart and charge
(defn create [dbd user-id cart charge-id]
  "Create a new receipt using the charge-id authorization and cart for user-id.
  Returns the receipt-id or nil if the capture failed."
  (let [result (card/capture charge-id)
    ]))

;; get user receipts

;; get store receipts

