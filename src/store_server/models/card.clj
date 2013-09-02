(ns store-server.models.card
  (:require [leveldb-clj.core :as db]
            [store-server.models.user :as user]
            [clj-stripe.common :as common]
            [clj-stripe.customers :as customers]
            [clj-stripe.cards :as cards]
            [clj-http.client :as client]))

(defonce stripe-token "sk_test_TO0mzOhZ992dsx5msaAWce1Y")
(defonce api-root     "https://api.stripe.com/v1")
(defonce client-options {:basic-auth stripe-token :throw-exceptions false :coerce :always :as :json})

;; 
;; TODO: implement customer logic on Aircart's side vs. Stripe
;; TODO: remove usage of clj-stripe lib horror
;; TODO: method for change billing?
;; store key: user_userid_stripe
;;

(defn get-stripe-id [dbd user-id]
  (let [stripe-id-bytes (db/get dbd (.getBytes (str "user_" user-id "_stripe")))]
    (if-not (nil? stripe-id-bytes)
      (String. stripe-id-bytes))))

(defn link-account [dbd user-id stripe-id]
  (db/put dbd (.getBytes (str "user_" user-id "_stripe")) (.getBytes stripe-id)))

(defn create [dbd user-id params]
  "Adds a new card to a user and make it the default card.
  The default logic and list storage currrently relies on Stripe.
  Returns the id of the card that was stored on Stripe."
  (let [new-card (common/card (common/number (params "number"))
                              (common/expiration (params "exp_month") (params "exp_year"))
                              (common/cvc (params "cvc"))
                              (common/owner-name (params "name"))
                              (common/zip (params "address_zip")))]
    (let [stripe-id (get-stripe-id dbd user-id)]
      (common/with-token stripe-token
        (if (nil? stripe-id)
          ;; if the user has no customer account on Stripe, create one
          (let [user-map (user/fetch dbd user-id)]
            ;; store customer with new card on Stripe
            ;; POSSIBLE BUG: does square accept a user with an incorrect card? if so, trouble.
            (let [stripe-response (common/execute (customers/create-customer new-card (customers/email (:email user-map)) (common/description (:name user-map))))]
              (when (nil? (:error stripe-response))
                ;; store customer id
                (link-account dbd user-id (:id stripe-response))
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
        (if (< 0 (:count (:cards stripe-response))) ; crashes if user key is present in db but not Stripe (got deleted there or test/live mode)
          { :default_card_id (:default_card stripe-response)
            :cards (vec (for [card (:data (:cards stripe-response))]
                       (select-keys card [:id :last4 :type :exp_month :exp_year :name :address_zip]))) })))))

(defn set-default [dbd user-id card-id]
  "Set card as default.
  Returns true if successful, false otherwise."
  (let [stripe-id (get-stripe-id dbd user-id)]
    (if-not (nil? stripe-id)
      (nil? (:error (common/with-token stripe-token
        (common/execute (customers/update-customer stripe-id { "default_card" card-id }))))))))

(defn delete [dbd user-id card-id]
  "Remove a card.
  Returns true if successful, false otherwise."
  (let [stripe-id (get-stripe-id dbd user-id)]
    (if-not (nil? stripe-id)
      (nil? (:error (common/with-token stripe-token
        (common/execute (cards/delete-card stripe-id (common/card card-id) (common/customer stripe-id)))))))))

;; TODO: handle exceptions with no :body in clj-http response (connection error etc.)

(defn charge [dbd user-id amount]
  "Charge a user using his default card for a given amount, without capturing
  the charge. Returns the charge-id or nil if unsuccessful (charge was declined
  or the customer has no card)."
  ; handle no stripe id as no card
  ; expired, missing, declined, invalid, processing issue
  (let [resp (client/post (str api-root "/charges") (merge client-options {:query-params
    { :amount   (int (* amount 100)) ; Stripe wants an integer in cents
      :currency "usd"
      :customer (get-stripe-id dbd user-id)
      :capture  false }})) body (resp :body)]
    (if-not (nil? (body :error))
      (do
        (when (not= "card_error" ((body :error) :type))
          (throw (Exception. ((body :error) :message)))) ;; TODO: add exception logger
        [nil {:type ((body :error) :type) :message ((body :error) :message)}])
      [(body :id) nil])))

(defn capture [charge-id]
  "Capture a charge.
  Returns true if successful, false otherwise."
  (let [resp (client/post (str api-root "/charges/" charge-id "/capture") client-options) body (resp :body)]
    (when-not (nil? (body :error))
      (throw (Exception. ((body :error) :message))))
    (nil? (body :error))))

(defn release [charge-id]
  "Release a charge.
  Returns true if successful, false otherwise."
  (let [resp (client/post (str api-root "/charges/" charge-id "/refund") client-options) body (resp :body)]
    (when-not (nil? (body :error))
      (throw (Exception. ((body :error) :message))))
    (nil? (body :error))))

(defn get-charge [charge-id]
  "Get details for a charge previosuly created." ;; TODO: verify it return nil on error / usage of return value
  (let [resp (client/get (str api-root "/charges/" charge-id) client-options) body (resp :body)]
    (if-not (nil? (body :error))
      (throw (Exception. ((body :error) :message)))
      body)))
