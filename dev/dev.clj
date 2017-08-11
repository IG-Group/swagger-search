(ns dev
  (:require
    [clojure.tools.namespace.repl :refer [disable-reload! refresh refresh-all]]
    [com.ig.swagger.search.standalone :as standalone]
    clojure.java.shell))

(disable-reload!)

(if-let [ns (find-ns 'com.ig.swagger.search.discovery.providers.consul)]
  (disable-reload! ns))

(def system (atom nil))

(defn start
  "Starts the current development system."
  []
  (standalone/start system (standalone/find-config-file)))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (standalone/stop system))

(defn go
  "Initializes the current development system and starts it running."
  []
  (start)
  :ok)

(defn reset []
  (stop)
  (refresh :after 'dev/go))

(defn run-index []
  ((:index-fn (:scheduled-indexer @system))))