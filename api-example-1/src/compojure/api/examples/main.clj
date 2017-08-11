(ns compojure.api.examples.main
  (:use [ring.util.response])
  (:require [ring.adapter.jetty :as jetty]
            [compojure.api.examples.handler :as handler]))

(defn start [& args]
  (jetty/run-jetty (var handler/app) {:port  3000
                                      :join? true}))