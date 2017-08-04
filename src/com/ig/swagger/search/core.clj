(ns com.ig.swagger.search.core
  "Namespace to bootstrap the application. Equivalent to the spring.xml"
  (:use [ring.util.response]
        [clojure.tools.logging :only [info warn error]])
  (:require [com.ig.swagger.search
             [routes :as routes]
             [index :as index]
             [collector :as collector]
             [discovery :as discovery]]
            [com.ig.swagger.search.util.ring-middleware :as ring-middleware]
            [com.ig.swagger.search.http-client.core :as http]))

(defn system-start
  "Given a config, it knows how to create and start a new system"
  [app-config]
  (info "Application starting up now ...")

  (let [index (index/create)
        provider (discovery/provider app-config)
        proxy (http/create-client (:proxy-client app-config))
        routes (-> (var routes/http-api)
                   ring-middleware/remove-context
                   (ring-middleware/wrap-with-additional-keys-in-req :index index
                                                                     :proxy-client proxy))
        scheduled-indexer (collector/schedule-indexing provider
                                                       (:collector app-config)
                                                       (partial index/replace index))
        system {:app-config        app-config
                :routes            routes
                :fetching-fn       provider
                :index             index
                :proxy             proxy
                :scheduled-indexer scheduled-indexer}]
    (info "Application started")
    system))

(defn system-stop
  "Given a system created by system-start, it knows how to stop it"
  [system]
    (info "Application stopping ...")
    (collector/destroy-component (:scheduled-indexer system))
    (http/destroy (:proxy system))
    (info "Application stopped"))