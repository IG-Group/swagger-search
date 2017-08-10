(ns com.ig.swagger.search.parser
  (:use [clojure.tools.logging :only [error info]])
  (:require [clojure.set]
            [ring.util.codec :as codec]
            [clojure.string :as string]
            [clojure.string :as str]))

(defn encode-ui-path
  [path operation-id]
  (str "/"
       (codec/url-encode (if (string/starts-with? path "/")
                           (.substring path 1)
                           path))
       "/"
       (-> operation-id
           (string/replace #"[/-]" "_")
           (string/replace #"[\{\}]" ""))))

(defn base-path-to-service-name [path]
  (str/capitalize (str/replace path "/" "")))

;;;
;;; V2 parsing
;;;

(defn stringify [key]
  (if key (name key)))

(defn get-controller-data
  [path [method operation]]
  (let [api-path (encode-ui-path (first (:tags operation ["default"]))
                                 (or (:operationId operation)
                                     (str (name method) "_" (name path))))]
    ;; TODO: add description
    {:method      (name method)
     :summary     (:summary operation)
     :ui-api-path api-path}))

(defn get-controller-methods [[path path-item]]
  (map (comp
         (fn [index-data] (assoc index-data :path (str "/" (stringify path))))
         (partial get-controller-data path)) path-item))

(defn get-controller-paths [swagger-paths]
  (mapcat get-controller-methods swagger-paths))

(defn index-data-for-v2 [{:keys [swagger-doc]}]
  (let [{:keys [paths swagger]} swagger-doc
        more-index-data (fn [controller-path]
                          (assoc controller-path
                            :servlet-context (:basePath swagger-doc)
                            :service-name (or (-> swagger-doc :info :title)
                                              (base-path-to-service-name (:basePath swagger-doc)))
                            :service-version (-> swagger-doc :info :version)
                            :swagger-version swagger))]
    {:index-data (map more-index-data (get-controller-paths paths))}))

;;;
;;; V1
;;;

(defn parse-swagger-controller [{:keys [basePath apis swaggerVersion resourcePath]}]
  (let [build-controller-fn (fn [controller-api operation]
                              (assoc (select-keys operation [:method :summary])
                                :path (:path controller-api)
                                :servlet-context basePath
                                :service-name (base-path-to-service-name basePath) ;; TODO: find if v1 has a service name
                                :swagger-version swaggerVersion
                                :ui-api-path (encode-ui-path resourcePath ;; this does not really work. we need a better solution (something in the UI?) to get the operation expanded
                                                             (:nickname operation (:description controller-api)))))]
    (mapcat (fn [controller-api]
              (map (partial build-controller-fn controller-api) (:operations controller-api)))
            apis)))

(defn index-data-for-v1 [{:keys [v1-api-docs]}]
  (let [controllers (vals v1-api-docs)]
    {:index-data
     (mapcat parse-swagger-controller controllers)}))