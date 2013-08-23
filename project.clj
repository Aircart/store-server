(defproject store-server "0.1.0-SNAPSHOT"
  :description "local Aircart server node"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-yaml "0.4.0"]
                 [org.clojars.aircart/leveldb-clj "0.1.1"]
                 [ring/ring-json "0.2.0"]
                 [compojure "1.1.5"]
                 [org.clojars.aircart/http-kit "2.2.0-SNAPSHOT"]
                 [clj-http "0.7.6"]
                 [factual/factual-clojure-driver "1.5.1"]
                 [org.clojars.aircart/clj-stripe "1.0.4"]] ;; can't wait to remove this lib
  :main store-server.core)
