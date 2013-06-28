(ns store-server.catalogs.factual
  (:require [factual.api :as fact]))
  (fact/factual! "SVaNREDwMAdbn2hjrxAyN0a9YD181eTe0XBgRnhN" "RMQ4RgqRCHf3xW8JaNrYiPdo5NYBmzqZu6t6wdoe")

(defn to-aircart [result]
  (print result)
  {:name (result :product_name)
   :image (result :image_urls)
   :price (* (result :avg_price) 100)})

(defn get-cpg [code]
  (to-aircart (first (fact/fetch {:table :products-cpg :filters {:upc code}}))))

(defn get-bulk [code] {})
