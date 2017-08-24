(ns com.ig.swagger.search.index
  (:refer-clojure :exclude [replace])
  (:require [clucy.core :as clucy]
            clojure.string
            cheshire.generate
            medley.core)
  (:import org.apache.lucene.analysis.en.EnglishAnalyzer))

(def ^{:private true} lucene-keys
  (medley.core/map-vals #(assoc % :type "string")
                        {:method                  {}
                         :path                    {}
                         :basePath                {:indexed false}
                         :swagger-version         {:indexed false}
                         :servlet-context         {:indexed false}
                         :ui-api-path             {:indexed false}
                         :ui-base-path            {:indexed false}
                         :parameters              {:stored false}
                         :responses               {:stored false}
                         :summary-and-description {:stored false}
                         :types                   {:stored false}
                         :service-name            {}
                         :all-content             {:stored false}}))

(defn- empty-index []
  (clucy.core/memory-index
    (assoc lucene-keys
      :*doc-with-meta?* false
      :*content* false
      :_id [:id {:type "string"}])))

(defn- details [endpoints]
  (set (map #(select-keys % [:swagger-version :servlet-context :ui-base-path :spec-path :service-name :service-version]) endpoints)))


(defn- map-stored [map-in]
  (merge {}
         (filter (complement nil?)
                 (map (fn [item]
                        (if (or (= nil (meta map-in))
                                (not= false
                                      (:indexed ((first item) (meta map-in)))))
                          item)) map-in))))

(defn- concat-values [map-in]
  (apply str (interpose " " (vals (map-stored map-in)))))

(defn- with-all-content
  "Like clucy :_content field, but using only the fields that are indexed. Clucy uses the fields that are stored."
  [m]
  (assoc m :all-content (concat-values (with-meta m lucene-keys))))

(defn- flattern-field [k]
  (fn [m]
    (update m k
            (fn [params]
              (clojure.string/join " " (mapcat vals params))))))

(defn- types-to-str [m]
  (update m :types #(clojure.string/join " " %)))

(def analyzer (EnglishAnalyzer. clucy.core/*version*))

(defn build-index [endpoints]
  (binding [clucy.core/*analyzer* analyzer]
    (let [index (empty-index)]
      (some->> endpoints
               (map (flattern-field :parameters))
               (map (flattern-field :responses))
               (map types-to-str)
               (map with-all-content)
               (apply clucy.core/add index))
      index)))

(defn build-state [{:keys [index-data all-data]}]
  {:index    (build-index index-data)
   :details  (details index-data)
   :all-data all-data})

(defn create []
  (atom (build-state nil)))

(defn replace [state collect-result]
  (reset! state (build-state collect-result)))

(defn search* [index query max-results]
  (binding [clucy.core/*analyzer* analyzer]
    (clucy/search index query max-results :default-field :all-content :default-operator :and)))

(defn search [state q]
  (search* (:index @state) q 100))

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