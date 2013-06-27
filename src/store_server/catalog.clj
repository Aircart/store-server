(ns store-server.catalog
  (:require [clj-yaml.core :as yaml]))

(defn get [code]
  ((yaml/parse-string (slurp "db/catalog.yaml")) code))
