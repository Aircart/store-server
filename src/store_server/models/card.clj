(ns store-server.models.card
  (:require [leveldb-clj.core :as db]
            [store-server.models.user :as user]
            [clj-stripe.common :as common]
            [clj-stripe.customers :as customers]
            [clj-stripe.cards :as cards]))

(def stripe-token "sk_test_TO0mzOhZ992dsx5msaAWce1Y")

;; 
;; TODO: implement customer logic on Aircart's side
;; TODO: remove usage of clj-stripe lib horror
;; TODO: method for change billing?
;; store key: user_userid_stripe
;;

(defn get-stripe-id [dbd user-id]
  (let [stripe-id-bytes (db/get dbd (.getBytes (str "user_" user-id "_stripe")))]
    (if-not (nil? stripe-id-bytes)
      (String. stripe-id-bytes))))

(defn create [dbd user-id params]
  "Adds a new card to a user and make it the default card.
  The default logic and list storage currrently relies on Stripe."
  (let [new-card (common/card (common/number (:number params))
                              (common/expiration (:exp_month params) (:exp_year params))
                              (common/cvc (:cvc params))
                              (common/owner-name (:name params))
                              (common/zip (:address_zip params)))]
    (let [stripe-id (get-stripe-id dbd user-id)]
      (common/with-token stripe-token
        (if (nil? stripe-id)
          ;; if the user has no customer account on Stripe, create one
          (let [user-map (user/fetch dbd user-id)]
            ;; store customer with new card on Stripe
            (let [stripe-response (common/execute (customers/create-customer new-card (customers/email (:email user-map)) (common/description (:name user-map))))]
              (if (nil? (:error stripe-response))
                ;; store customer id
                (db/put dbd (.getBytes (str "user_" user-id "_stripe")) (.getBytes (:id stripe-response)))
                ;; return stripe card id
                (:id (((stripe-response :cards) :data) 0)))))
          ;; otherwise, just store a new card
          (let [stripe-response (common/execute (cards/create-card new-card (common/customer stripe-id)))]
            (if (nil? (:error stripe-response))
              (:id stripe-response))))))))

(defn fetch-all [dbd user-id]
  "Fetches all the cards for a given user,
  as well as the ID of the default card."
  (let [stripe-id (get-stripe-id dbd user-id)]
    (if-not (nil? stripe-id)
      ;; if no card, return nil
      (let [stripe-response (common/with-token stripe-token
        (common/execute (customers/get-customer stripe-id)))]
        (if (< 0 (:count (:cards stripe-response)))
          { :default_card (:default_card stripe-response)
            :cards (vec (for [card (:data (:cards stripe-response))]
                       (select-keys card [:id :last4 :type :exp_month :exp_year :name :address_zip]))) })))))

;; set default
(defn set-default [dbd user-id card-id]
  (let [stripe-id (get-stripe-id dbd user-id)]
    (if-not (nil? stripe-id)
      (nil? (:error (common/with-token stripe-token
        (common/execute (customers/update-customer stripe-id { "default_card" card-id }))))))))

;; remove card
(defn delete [dbd user-id card-id]
  (let [stripe-id (get-stripe-id dbd user-id)]
    (if-not (nil? stripe-id)
      (nil? (:error (common/with-token stripe-token
        (common/execute (cards/delete-card stripe-id (common/card card-id) (common/customer stripe-id)))))))))
