(ns store-server.core
  (:gen-class)
  (:require [store-server.web :as web])
  (:use ring.adapter.jetty))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Running server on port 8080")
  (run-jetty #'web/app {:port 8080}))
