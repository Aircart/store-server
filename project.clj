(defproject store-server "0.1.0-SNAPSHOT"
  :description "local Aircart server node"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-yaml "0.4.0"]
                 [ring/ring-jetty-adapter "1.2.0-RC1"]
                 [ring/ring-json "0.2.0"]
                 [compojure "1.1.5"]]
  :main store-server.core)
