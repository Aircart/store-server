(ns store-server.catalogs.local
  (:require [clj-yaml.core :as yaml]))

(defn get-cpg [code]
  (def data (yaml/parse-string (slurp "db/cpg.yaml")))
  (data (if (contains? data code)
    code
    :default)))

(defn read-bulk []
  (yaml/parse-string (slurp "db/bulk.yaml")))

(defn get-bulk [code] ; TODO: dry up
  ((read-bulk) code))
