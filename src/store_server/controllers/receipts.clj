(ns store-server.controllers.receipts
  (:require [store-server.models.receipt :as receipt]))

(defn list-receipts [user-id dbd]
  { :status 200
    :body   (receipt/fetch-user-receipts dbd user-id) })

(defn get-receipt [receipt-id user-id dbd]
  (if-let [receipt (receipt/fetch-receipt-details dbd receipt-id)]
    { :status 200 
      :body   receipt }
    { :status 404 }))
