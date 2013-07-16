(ns store-server.catalogs.local
  (:require [clj-yaml.core :as yaml]))

(defn get-cpg [code]
  (def data (yaml/parse-string (slurp "db/cpg.yaml")))
  (data (if (contains? data code)
    code
    :default)))

(defn get-bulk [code] ; TODO: dry up
  ((yaml/parse-string (slurp "db/bulk.yaml")) code))
