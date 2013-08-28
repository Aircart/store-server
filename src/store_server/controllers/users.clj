(ns store-server.controllers.users
  (:require [clj-http.client :as client]
            [store-server.models.user :as user]))

(defn auth-facebook-user [user-id facebook_access_token dbd]
  (let [response (client/get "https://graph.facebook.com/me" {:query-params {"access_token" facebook_access_token}
                                                              :as :json
                                                              :throw-exceptions false})] ; TODO: handle exceptions
    ;; switch fb's response
    (if (= (:status response) 200)
      (let [db-user (user/fetch dbd user-id) facebook-fields (:body response)] ; SECURITY: replace user-id with (:id (:body response))
        (if (nil? db-user)
          (do
            ;; save new user
            (user/save dbd facebook-fields facebook_access_token)
            {:status 201})
          (do
            ;; update access token on existing user
            (user/update-token dbd db-user facebook-fields facebook_access_token)
            {:status 200})))
      {:status 401})))

(defn get-details [user-id dbd]
  "Get user details for the checkpoint.
  Only includes first name and picture url."
  (if-let [user-map (user/fetch dbd user-id)]
    { :status 200 
      :body   (merge
                (select-keys user-map [:first_name])
                {:picture_url (format "http://graph.facebook.com/%s/picture?width=200&height=200" user-id)})}
    { :status 404 }))
