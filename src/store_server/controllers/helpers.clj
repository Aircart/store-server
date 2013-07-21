(ns store-server.controllers.helpers
  (:require [store-server.models.user :as user]))

;; TODO: replace with macro
(defn with-authentication [req db-descriptor callback]
  ((fn [matches]
    (if (nil? matches)
      ;; misformed token
      {:status 401}
      (if (nil? (user/fetch-token db-descriptor (matches 1)))
        ;; could not find token
        {:status 401}
        ;; authentication successful
        (callback))))
    ((fn [auth-header]
      (if-not (nil? auth-header) (re-find #"Token token=\"(\w+)\"" auth-header)))
      ((:headers req) "authorization"))))
