(ns com.ig.swagger.search.parser
  (:use [clojure.tools.logging :only [error info]])
  (:require [clojure.set]
            [ring.util.codec :as codec]
            [clojure.string :as string]))

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
  (let [{:keys [basePath paths swagger]} swagger-doc
        more-index-data (fn [controller-path]
                          (assoc controller-path
                            :servlet-context basePath       ;; Why not use the title instead of the servlet-context?
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