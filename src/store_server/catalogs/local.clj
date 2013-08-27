(ns store-server.catalogs.local
  (:require [clj-yaml.core :as yaml]))

;; TODO: use promise to store results and avoid re-reading files?

(defn get-cpg [code]
  (def data (yaml/parse-string (slurp "data/cpg.yaml")))
  (or (data (keyword code)) (data :default)))

(defn read-bulk []
  (yaml/parse-string (slurp "data/bulk.yaml")))

(defn get-bulk [code]
  ((read-bulk) (keyword code)))
