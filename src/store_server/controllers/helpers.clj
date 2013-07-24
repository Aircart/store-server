(ns store-server.controllers.helpers
  (:require [store-server.models.user :as user]))

;; TODO: replace with macro
(defn with-authentication [req db-descriptor callback]
  ((fn [matches]
    (if (nil? matches)
      ;; misformed token
      {:status 400}
      ((fn [user-id]
        (if (nil? user-id)
          ;; could not find token
          {:status 401}
          ;; authentication successful
          (callback user-id)))
        (user/fetch-id db-descriptor (matches 1)))))
    ((fn [auth-header]
      (if-not (nil? auth-header) (re-find #"Token token=\"(\w+)\"" auth-header)))
      ((:headers req) "authorization"))))
