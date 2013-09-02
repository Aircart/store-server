(ns store-server.controllers.helpers
  (:require [store-server.models.user :as user]))

;; TODO: replace with macro?
(defn with-authentication [req db-descriptor callback]
  (if-let [matches (some->> ((:headers req) "authorization") (re-find #"Token token=\"(\w+)\""))]
    (if-let [user-id (user/fetch-id db-descriptor (matches 1))]
      ;; authentication successful
      (callback user-id)
      ;; could not find token
      {:status 401})
    ;; misformed token
    {:status 400}))
