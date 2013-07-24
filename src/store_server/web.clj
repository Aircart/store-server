(ns store-server.web
  (:use compojure.core
        ring.middleware.json
        ring.util.response
        store-server.controllers.helpers)
  (:require [store-server.catalogs.local :as catalog]
            [store-server.models.user :as user]
            [clj-http.client :as client]))

(defn simple-logging-middleware [appw]
  (fn [req]
    (println req)
    (appw req)))

(defn load-with-descriptor [db-descriptor]
  (defroutes handler

    (PUT "/users/:user-id" [user-id facebook_access_token]
      ((fn [response]
        ;; switch fb's response
        (if (= (:status response) 200)
          ((fn [db-user facebook-fields]
            (if (nil? db-user)
              (do
                ;; save new user
                (user/save db-descriptor facebook-fields facebook_access_token)
                {:status 201})
              (do
                ;; update access token on existing user
                (user/update-token db-descriptor db-user facebook-fields facebook_access_token)
                {:status 200}))
              ;; TODO: set-up new cart
            )
            (user/fetch db-descriptor user-id) (:body response)) ; SECURITY: replace user-id with (:id (:body response))
          {:status 401}))
        ;; connect to facebook 
        (client/get "https://graph.facebook.com/me" {:query-params {"access_token" facebook_access_token}
                                                     :as :json ; output coercion
                                                     :throw-exceptions false})))

    (GET "/scale-products" []
      (response (catalog/read-bulk)))

    ;; TODO: namescape authenticated routes, separate routes and controller logic

    (POST "/scans" [code :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          ;; TODO: add item already present, return 200
          { :status 201
            :headers { "Location" (str "/cart-item/" code) }
            :body (catalog/get-cpg (keyword code)) })))

    (POST "/scale-scans" [code :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          ;; TODO: add item already present, return 200
          { :status 201
            :headers { "Location" (str "/cart-item/" code) }
            :body (catalog/get-bulk (keyword code)) })))

    (PUT "/cart-item/:code" [code quantity :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          ;; TODO: update item's quantity in cart
          { :status 204 })))

    (DELETE "/cart-item/:code" [code :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          ;; TODO: remove item from cart
          { :status 204 }))))

  (def app
    (-> handler
      wrap-json-params
      wrap-json-response
      simple-logging-middleware)))
