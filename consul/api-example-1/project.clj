(defproject metosin/compojure-api-examples "1.0.1"
            :description "Compojure-api-examples"
            :dependencies [[org.clojure/clojure "1.8.0"]
                           [metosin/compojure-api "1.1.11"]
                           [javax.servlet/javax.servlet-api "3.1.0"]
                           [ring/ring-jetty-adapter "1.6.1"]]
            :main compojure.api.examples.main/start)
