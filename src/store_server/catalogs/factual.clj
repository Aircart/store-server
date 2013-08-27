(ns store-server.catalogs.factual
  (:require [factual.api :as fact]
            [store-server.catalogs.local :as local]))

(fact/factual! "SVaNREDwMAdbn2hjrxAyN0a9YD181eTe0XBgRnhN" "RMQ4RgqRCHf3xW8JaNrYiPdo5NYBmzqZu6t6wdoe")

(defn to-aircart [result]
  {:name (result "product_name")
   :image (first (result "image_urls"))
   :price (* (or (result "avg_price") 10) 100)})

(defn get-cpg [code]
  (if-let [results (first (fact/fetch { :table :products-cpg
                                        :filters { :upc code } }))]
    (to-aircart results)
    (local/get-cpg :default)))

(defn read-bulk [] (local/read-bulk)) ; dry up with redefinitions?

(defn get-bulk [code] (local/get-bulk code)) ; dry up with redefinitions?
