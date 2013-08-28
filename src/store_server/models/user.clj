(ns store-server.models.user
  (:require [leveldb-clj.core :as db]))

(defn facebook-to-bytes [facebook-fields access-token]
  (.getBytes (pr-str (merge facebook-fields {:facebook_token access-token}))))

(defn bytes-to-map [bytes] ; TODO: move into utility function
  (read-string (String. bytes)))

(defn fetch [db-descriptor user-id]
  "fetches user map with user id"
  ((fn [user-bytes]
    (if-not (nil? user-bytes) (bytes-to-map user-bytes)))
    (db/get db-descriptor (.getBytes (str "user_" user-id)))))

(defn fetch-id [db-descriptor facebook-token]
  "fetch user id with token"
  ((fn [user-id]
    (if-not (nil? user-id) (String. user-id)))
    (db/get db-descriptor (.getBytes (str "user_token_" facebook-token)))))

(defn save [db-descriptor facebook-fields facebook-token]
  (with-open [db-batch (db/create-write-batch db-descriptor)]
    ;; save user fields
    (db/batch-put db-batch (.getBytes (str "user_" (:id facebook-fields))) (facebook-to-bytes facebook-fields facebook-token))
    ;; save token index
    (db/batch-put db-batch (.getBytes (str "user_token_" facebook-token)) (.getBytes (:id facebook-fields)))
    ;; write
    (db/write-batch db-descriptor db-batch)))

(defn update-token [db-descriptor user-map facebook-fields facebook-token]
  (with-open [db-batch (db/create-write-batch db-descriptor)]
    ;; overwrite user fields, this can update other facebook fields
    (db/batch-put db-batch (.getBytes (str "user_" (:id facebook-fields))) (facebook-to-bytes facebook-fields facebook-token))
    ;; remove expired token index
    (db/batch-delete db-batch (.getBytes (str "user_token_" (:facebook_token user-map))))
    ;; save updated token index
    (db/batch-put db-batch (.getBytes (str "user_token_" facebook-token)) (.getBytes (:id facebook-fields)))
    ;; write
    (db/write-batch db-descriptor db-batch)))
