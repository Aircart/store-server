(ns store-server.controllers.checkouts
  (:use org.httpkit.server)
  (:require [store-server.models.cart :as cart]
            [store-server.models.card :as card]
            [store-server.models.receipt :as receipt]))

(defonce checkpoint-channel (atom nil))
(defonce user-channels (atom {}))

(defn checking-out? [user-id]
  "This method is used to secure cart change requests."
  ;; TODO: match cart-id
  (not (nil? (@user-channels user-id))))

(defn checkpoint-subscribe [req]
  "Checkpoint method to subscribe to all incoming checkouts and open channel."
  (with-channel req channel
    ;; close zombie checkpoint (warning: may cause race condition)
    ;; TODO: call on-close fn vs. close, disable frivolous notifications
    ;; how about that zombie channel closing later? callback should be disabled
    (some-> @checkpoint-channel (close))
    ;; update atom
    (reset! checkpoint-channel channel)
    (on-close channel (fn [status status-code]
      ;; close all open user checkouts (wether close is checkpoint or server)
      (dorun (pmap #(close (% :chan) 1001) (vals @user-channels)))
      ;; update atom
      (reset! checkpoint-channel nil))))) ;; TODO: reverse order to avoid frivolous notification?

(defn user-checkout [user-id req dbd]
  "Main method handling user checkout, initiated by the user. Cannot execute
  if the checkpoint is not connected."
  (if (nil? @checkpoint-channel)
    {:status  503
     :headers {"Reason-Phrase" "The checkpoint is not online"}}
    (if-not (nil? (@user-channels user-id))
      {:status  409
       :headers {"Reason-Phrase" "You already have a checkout in progress"}}
       ;; TODO: overwrite checkout instead?
      (let [cart-id (cart/get-current-cart-id dbd user-id) cart (cart/fetch-with-id dbd cart-id)]
        (if (empty? (-> cart :items))
          {:status  403
           :headers {"Reason-Phrase" "You don't have a cart or your cart is empty"}}
          ;; place lock on credit card
          (let [total-map (cart/compute-total cart)
               [charge-id error] (card/charge dbd user-id (total-map :total))]
            (if-not (nil? error)
              (if (not= "card_error" (error :type)) ;; or could be 500 if invalid_request_error
                {:status 502
                 :headers {"Reason-Phrase" "Payment Gateway Error"}}
                {:status  402
                 :headers {"Reason-Phrase" (error :message)}})
              ;; TODO: ensure there is a timer after which channel is closed, before hold is released
              (with-channel req channel
                (on-close channel (fn [status status-code]
                  (if-not (and (= :server-close status) (= 1000 status-code))
                    ;; release hold if it was anything but a completion
                    (card/release charge-id))
                  ;; remove from atom
                  (swap! user-channels #(dissoc % user-id))
                  ;; notify checkpoint (use watch? or agents?)
                  (send! @checkpoint-channel (format "%s: close" user-id)))) ; TODO: verify checkpoint-channel open to avoid frivolous notification?
                ;; append to atom
                (swap! user-channels #(assoc % user-id {:chan      channel
                                                        :cart-id   cart-id
                                                        :total-map total-map
                                                        :charge-id charge-id}))
                ;; notify checkpoint (use watch? or agents?)
                (send! @checkpoint-channel (format "%s: open" user-id))))))))))

(defn finalize [user-id req dbd]
  "Checkpoint method to finalize user checout and perform verifications,
  if any."
  ;; TODO: implement abort hooks from other methods, add finalization channel to atom
  (let [userc (@user-channels user-id)]
    (if (nil? userc) ; use if-let?
      {:status 404}
      ;; TODO: implement websockets/verification
      ; generate receipt with payment hold
      (let [receipt-id (receipt/create dbd user-id (userc :cart-id) (userc :total-map) (userc :charge-id))]
        (if (nil? receipt-id)
          {:status 500}
          (do
            (send! (userc :chan) (format "receipt: /receipts/%s" receipt-id))
            (close (userc :chan) 1000)
            {:status 204}))))))

(defn abort [user-id]
  "Checkpoint method to abort a user checkout before finalization."
  (let [userc (@user-channels user-id)]
    (if (nil? userc)
      {:status 404}
      (do
        (close (userc :chan) 1001)
        {:status 204}))))

(defn list-purchases [user-id dbd]
  "Get the content (without prices) of a user's cart currently checking out.
  This will get the cart-id from the open channel, and thus is still exact if
  the user starts a new cart in parallel."
  (if-let [userc (@user-channels user-id)]
    ;(let [cart (cart/fetch-with-id dbd (userc :cart-id))]
    {:status 200
       ;:body (cart/attach-item-details (cart :store) (cart :items))})
     :body (cart/select-purchases (cart/fetch-with-id dbd (userc :cart-id)))}
    {:status 404}))

;; self-finalization -- UNIMPLEMENTED

  ;; verify QR code

    ;; charge card

      ;; generate receipt -- receipt url returned in response

    ;; problem charging card


;; use http server on checkpoint to maintain state
;; activate unattended mode -- SECURITY: generate new checkpoint QR each time

  ;; push status change to active checkouts devices -- needed to activate camera
