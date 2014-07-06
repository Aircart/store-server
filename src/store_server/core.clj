(ns store-server.core
  (:gen-class)
  (:require [store-server.web :as web]
            [leveldb-clj.core :as db])
  (:use org.httpkit.server))
        
(defn -main
  "Runs the Aircart store server."
  [& args]

  (println "Opening LevelDB file in db/main")
  (let [main-db (db/open (System/getenv "AIRCART_DB"))]

    (println "Running server on port 8080")
    (run-server (web/load-with-descriptor main-db) {:port 8080})))

;; TODO: add Runtime.getRuntime().addShutdownHook to close db file
;;       use atom/promise to pass db-descriptor around?
;; see:  http://stackoverflow.com/questions/10855559/shutdown-hook-doesnt-fire-when-running-with-lein-run
;;       http://docs.oracle.com/javase/1.4.2/docs/guide/lang/hook-design.html

;; TODO: set up as a daemon for prod
