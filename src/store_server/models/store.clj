(ns store-server.models.store
  (:require [clj-yaml.core :as yaml]))

(defn fetch [store-id]
  (let [data (yaml/parse-string (slurp "data/stores.yaml"))]
    (data (keyword store-id))))
