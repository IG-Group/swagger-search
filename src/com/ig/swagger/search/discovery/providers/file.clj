(ns com.ig.swagger.search.discovery.providers.file
  (:require [clojure.java.io :as io]))

(defn from-uri-or-file
  [{:keys [uri-or-file]}]
  (fn []
    (clojure.string/split-lines
      (slurp uri-or-file))))

(defn from-classpath
  [{:keys [classpath-file]}]
  (fn []
    (clojure.string/split-lines
      (slurp (io/file (io/resource classpath-file))))))

(defn server-list
  [{:keys [server-list]}]
  (fn []
    (map name server-list)))