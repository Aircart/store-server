(ns store-server.core
  (:gen-class)
  (:require [store-server.web :as web]
            [leveldb-clj.core :as db])
  (:use ring.adapter.jetty))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]

  (println "Opening LevelDB file in db/main")
  (with-open [main-db (db/open "db/main")]

    (println "Running server on port 3000")
    (run-jetty (web/load-with-descriptor main-db) {:port 3000})))
