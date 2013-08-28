(ns store-server.web
  (:use compojure.core
        ring.middleware.json
        ring.util.response
        store-server.controllers.helpers)
  (:require [store-server.catalogs.local :as catalog]
            [store-server.controllers.users :as users]
            [store-server.controllers.stores :as stores]
            [store-server.controllers.carts :as carts]
            [store-server.controllers.cards :as cards]
            [store-server.controllers.checkouts :as checkouts]
            [store-server.controllers.receipts :as receipts]))

;; models to add:
;; - models/event
;; - models/employee - employee login via QR code

;; LOGIC/SECURITY concern:
;; - freeze prices on cart (change during session, fixed prices on receipt)
;; - forbid cart change actions if user has ongoing checkout (on specific cart-id)

(defn simple-logging-middleware [appw]
  (fn [req]
    (println req)
    (appw req)))

(defn load-with-descriptor [dbd]
  (defroutes handler

    ;; scale methods
    ;;

    (GET "/scale-products" []
      (response (catalog/read-bulk))) ;; TODO: move into a controller and remove catalog require here


    ;; user methods
    ;; TODO: namescape authenticated routes
    ;;

    (GET "/stores/:store-id" [store-id]
      (stores/get-store store-id))

    (PUT "/users/:user-id" [user-id facebook_access_token]
      (users/auth-facebook-user user-id facebook_access_token dbd))

    (POST "/carts" [store_id location :as req]
      (with-authentication req dbd
        #(carts/create-cart store_id % dbd)))

    (GET "/cart" [:as req]
      (with-authentication req dbd
        #(carts/get-current-cart % dbd)))

    (POST "/scans" [barcode :as req]
      (with-authentication req dbd
        #(carts/add-cpg-item barcode % dbd)))

    (POST "/scale-scans" [plu weight :as req]
      (with-authentication req dbd
        #(carts/add-bulk-item plu weight % dbd)))

    (PUT "/cart-items/:code" [code quantity :as req]
      (with-authentication req dbd
        #(carts/change-item-quantity code quantity % dbd)))

    (DELETE "/cart-items/:code" [code :as req]
      (with-authentication req dbd
        #(carts/remove-item code % dbd)))

    (GET "/cards" [:as req]
      (with-authentication req dbd
        #(cards/list-cards % dbd)))

    (POST "/cards" [:as req]
      (with-authentication req dbd
        #(cards/add-card (:params req) % dbd)))

    (PUT "/default-card" [id :as req]
      (with-authentication req dbd
        #(cards/set-default-card id % dbd)))

    (DELETE "/cards/:id" [id :as req]
      (with-authentication req dbd
        #(cards/delete-card id % dbd)))

    (GET "/receipts/create" [:as req] ; websocket
      (with-authentication req dbd
        #(checkouts/user-checkout % req dbd)))

    (GET "/receipts" [:as req]
      (with-authentication req dbd
        #(receipts/list-receipts % dbd)))

    (GET "/receipts/:id" [id :as req]
      (with-authentication req dbd
        #(receipts/get-receipt id % dbd)))

    ;; checkpoint methods
    ;; TODO: add checkpoint auth
    ;;

    (GET "/checkouts" [:as req] ; websocket
      (checkouts/checkpoint-subscribe req))

    (GET "/checkouts/:user-id/finalize" [user-id :as req] ; websocket
      (checkouts/finalize user-id req dbd))

    (DELETE "/checkouts/:user-id" [user-id]
      (checkouts/abort user-id))

    (GET "/purchases/:user-id" [user-id]
      (checkouts/list-purchases user-id dbd))

    (GET "/users/:user-id" [user-id]
      (users/get-details user-id dbd)))


  (def app
    ;; TODO: use api wrapper with standard form-type params?
    (-> handler
      wrap-json-params
      wrap-json-response
      simple-logging-middleware)))
