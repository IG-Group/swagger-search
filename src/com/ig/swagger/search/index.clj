(ns com.ig.swagger.search.index
  (:refer-clojure :exclude [keys replace])
  (:require [clucy.core :as clucy]
            cheshire.generate
            medley.core
            [schema.core :as s]))

(def ^{:private true} keys
  [:method :path :basePath :swagger-version :servlet-context :ui-api-path :ui-base-path :spec-path :summary])

(defn with-val [ks v]
  (zipmap ks (repeat v)))

(defn- empty-index []
  (clucy.core/memory-index
    (assoc (with-val keys {:type "string"})
      :_id [:id {:type "string"}])))

(defn- details [endpoints]
  (set (map #(select-keys % [:swagger-version :servlet-context :ui-base-path :spec-path]) endpoints)))

(defn- build-index [endpoints]
  (let [index (empty-index)]
    (when endpoints
      (apply clucy.core/add index endpoints))
    index))

(defn build-state [{:keys [index-data all-data]}]
  {:index (build-index index-data)
   :details (details index-data)
   :all-data all-data})

(defn create []
  (atom (build-state nil)))

(defn replace [state collect-result]
  (reset! state (build-state collect-result)))

(defn search [state q]
  (clucy/search (:index @state) q 100))

(defn get-swagger-services [state]
  (:details @state))

(cheshire.generate/add-encoder java.lang.Throwable
                               ;; TODO: serialize stacktrace?
                               (fn [c jsonGenerator]
                                 (.writeString jsonGenerator (str c))))

(cheshire.generate/add-encoder clojure.lang.IDeref
                               (fn [c jsonGenerator]
                                 (.writeString jsonGenerator (str c))))

(defn parsing-result-for [state service-url]
  (medley.core/find-first (comp (partial = service-url) :service-url)
                          (:all-data @state)))