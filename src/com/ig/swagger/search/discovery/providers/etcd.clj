(ns com.ig.swagger.search.discovery.providers.etcd
  (:require [etcd-clojure.core :as etcd-clj]
            [clojure.tools.logging :as log]))

(defn create-etcd [{:keys [etcd]}]
  (etcd-clj/connect! (:host etcd) (:port etcd))
  (fn []
    (log/info "etcd provider is using" etcd)
    (->>
      (etcd-clj/list (:prefix etcd))
      (map :key)
      (map etcd-clj/list)
      (map first)
      (map :value)
      (map (partial str "http://"))
      set
      seq)))