(ns store-server.models.user
  (:require [leveldb-clj.core :as db]))

(defn fetch [db-descriptor user-id]
  ((fn [user]
    (if-not (nil? user) (String. user)))
    (db/get db-descriptor (.getBytes (str "user_" user-id)))))

(defn fetch-token [db-descriptor facebook-token]
  ((fn [token]
    (if-not (nil? token) (String. token)))
    (db/get db-descriptor (.getBytes (str "user_token_" facebook-token)))))

(defn save [db-descriptor facebook-fields facebook-token]
  (with-open [db-batch (db/create-write-batch db-descriptor)]
    ;; save user fields
    (db/batch-put db-batch (.getBytes (str "user_" (:id facebook-fields))) (.getBytes facebook-token))
    ;; save token index
    (db/batch-put db-batch (.getBytes (str "user_token_" facebook-token)) (.getBytes (:id facebook-fields)))
    ;; write
    (db/write-batch db-descriptor db-batch)))

(defn update-token [db-descriptor facebook-fields updated-token expired-token]
  (with-open [db-batch (db/create-write-batch db-descriptor)]
    ;; overwrite user fields
    (db/batch-put db-batch (.getBytes (str "user_" (:id facebook-fields))) (.getBytes updated-token))
    ;; remove expired token index
    (db/batch-delete db-batch (.getBytes (str "user_token_" expired-token)))
    ;; save updated token index
    (db/batch-put db-batch (.getBytes (str "user_token_" updated-token)) (.getBytes (:id facebook-fields)))
    ;; write
    (db/write-batch db-descriptor db-batch)))
