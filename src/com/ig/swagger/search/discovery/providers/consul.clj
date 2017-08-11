(ns com.ig.swagger.search.discovery.providers.consul
  (:require [consul.core :as consul]
            [clojure.tools.logging :as log]))

(defn consul-index
  [conn method endpoint params]
  (let [{:keys [body headers]} (consul/consul conn method endpoint params)]
    (assoc (consul/headers->index headers) :body body)))

(defn catalog-services
  ([conn]
   (catalog-services conn {}))
  ([conn params]
   (:body (consul-index conn :get [:catalog :services] {:query-params params}))))

(defn key-not-blank [key]
  (comp (complement clojure.string/blank?) key))

(defn create-consul [{:keys [consul]}]
  (let [config {:server-name (:host consul)
                :server-port (:port consul)}]
    (fn []
      (log/info "consul provider is using" config)
      (->>
        (catalog-services config)
        keys
        (map #(consul/catalog-service config %))
        (map first)
        (filter (key-not-blank :service-address))
        (filter :service-port)
        (map (fn [{:keys [service-address service-port]}] (str "http://" service-address ":" service-port)))
        distinct))))