(ns store-server.web
  (:use compojure.core
        ring.middleware.json
        ring.util.response
        store-server.controllers.helpers)
  (:require [store-server.catalogs.local :as catalog]
            [store-server.models.user :as user]
            [store-server.models.cart :as cart]
            [store-server.models.card :as card]
            [clj-http.client :as client]))

(defn simple-logging-middleware [appw]
  (fn [req]
    (println req)
    (appw req)))

(def tax_rate 5.75)

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

    (POST "/carts" [store_id location :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (cart/create db-descriptor user-id)
          { :status 201
            :body { :tax_rate tax_rate }})))

    (GET "/cart" [:as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (let [cart (cart/fetch db-descriptor user-id)]
            (if (nil? cart)
              { :status 404 }
              (response {
                :store_id "aircart_lab"
                :tax_rate tax_rate
                :items    (vec (for [[k v] cart]
                            (if (< 5 (.length (name k))) ; test wether plu or barcode
                              (merge { :barcode k
                                       :quantity v }
                                     (catalog/get-cpg (keyword k)))
                              (merge { :plu k
                                       :weight v }
                                     (catalog/get-bulk (keyword k))))))}))))))

    (POST "/scans" [barcode :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (let [code (keyword barcode)]
            (let [existed? (cart/change db-descriptor user-id code)]
              (if-not (nil? existed?) ; TODO: add error code for scan with no cart
                (if existed?
                  { :status 204 }
                  { :status 201
                    :headers { "Location" (str "/cart-items/" barcode) }
                    :body (catalog/get-cpg code) })))))))

    (POST "/scale-scans" [plu weight :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (let [code (keyword plu)]
            (let [existed? (cart/change db-descriptor user-id code :qt weight)]
              (if-not (nil? existed?) ; TODO: add error code for scan with no cart
                (if existed?
                  { :status 204 }
                  { :status 201
                    :headers { "Location" (str "/cart-items/" plu) }
                    :body (catalog/get-bulk code) })))))))

    (PUT "/cart-items/:code" [code quantity :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          ;; this will create an item if none was scanned before...
          (if-not (nil? (cart/change db-descriptor user-id (keyword code) :qt quantity :increment? false))
            { :status 204 }))))

    (DELETE "/cart-items/:code" [code :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (if-not (nil? (cart/change db-descriptor user-id (keyword code) :qt 0))
            { :status 204 }))))

    (GET "/cards" [:as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (let [resp-map (card/fetch-all db-descriptor user-id)]
            (if (nil? resp-map)
              { :status 204 }
              (response resp-map))))))

    (POST "/cards" [:as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (let [card-id (card/create db-descriptor user-id (:params req))]
            (if (nil? card-id)
              { :status 402 }
              { :status 201
                :headers { "Location" (str "/cards/" card-id) } })))))

    (PUT "/default-card" [id :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (if (card/set-default db-descriptor user-id id)
            { :status 200 }
            { :status 404 }))))

    (DELETE "/cards/:id" [id :as req]
      (with-authentication req db-descriptor
        (fn [user-id]
          (if (card/delete db-descriptor user-id id)
            { :status 200 }
            { :status 404 }))))

    (POST "/checkouts"
      (with-authentication req db-descriptor
        (fn [user-id]
          ))))

  (def app
    (-> handler
      wrap-json-params
      wrap-json-response
      simple-logging-middleware)))
