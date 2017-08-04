(ns com.ig.swagger.search.collector
  (:require [com.ig.swagger.search.parser :as parser]
            [schejulure.core :as schedule]
            [com.ig.swagger.search.http-client.core :as http])
  (:use [clojure.tools.logging :only [info warn error]]))

(comment

  {:service-url                 "http://bip1.test.iggroup.local/bonusmanagement"
   :possible-swagger-doc-suffix ["/v2/api-docs/" "/swagger/swagger.json" "/api-docs"]
   :possible-swagger-ui-suffix  ["/swagger/ui/index.html" "/swagger-ui.html" "/swagger/index.html" "/swagger/ui/swagger-ui.html"]
   :swagger-doc-url             "http://bip1.test.iggroup.local/bonusmanagement/api-docs"
   :swagger-ui-url              "http://bip1.test.iggroup.local/bonusmanagement/swagger/index.html"
   :swagger-doc                 {,,,,}
   :v1-api-docs                 {"url" {,,,}}
   :index-data                  [{:path            "/search"
                                  :spec-path       "http://ip1/a-tomcat-service/api-docs" ;; swagger-doc-url
                                  :swagger-version "1.2"
                                  :summary         "Search for API"
                                  :method          "GET"
                                  :ui-api-path     "http://ip1/a-tomcat-service/swagger/index.html#!/abc/operationSearch"
                                  :ui-base-path    "http://ip1/a-tomcat-service/swagger/index.html#!" ;; swagger-ui-url
                                  :servlet-context "/a-tomcat-service"}]
   :error                       ["..."]})

;;;
;;; general
;;;

(defn ok? [result]
  (and (not (:error result))
       (= 200 (-> result :status :code))))

(defn do-multiple-requests
  [http-client {:keys [base-url suffixes]}]
  (let [responses (map #(http-client {:method :get :url (str base-url %)}) suffixes)]
    [(filter ok? responses) (remove ok? responses)]))

(defn first-ok [responses]
  (if-let [first-ok (ffirst responses)]
    (select-keys first-ok [:body :url])
    {:error [{:type    :http
              :context (fnext responses)}]}))

;;;
;;; Swagger docs
;;;

(defn find-swagger-docs [http-client {:keys [service-url possible-swagger-doc-suffix]}]
  (-> (do-multiple-requests http-client {:base-url service-url
                                         :suffixes possible-swagger-doc-suffix})
      first-ok
      (clojure.set/rename-keys {:body :swagger-doc
                                :url  :swagger-doc-url})))

(defn collect-additional-controllers-for-v1 [http-client {:keys [swagger-doc-url swagger-doc]}]
  (let [controllers (keep :path (:apis swagger-doc))
        [docs errors] (do-multiple-requests http-client {:base-url swagger-doc-url
                                                         :suffixes controllers})]
    (if (seq errors)
      {:error errors}
      {:v1-api-docs (zipmap controllers
                            (map :body docs))})))

;;;
;;; Swagger UI related data
;;;

(defn find-swagger-ui [http-client {:keys [service-url possible-swagger-ui-suffix]}]
  (-> (do-multiple-requests http-client {:base-url service-url
                                         :suffixes possible-swagger-ui-suffix})
      first-ok
      (dissoc :body)
      (clojure.set/rename-keys {:url   :swagger-ui-url
                                :error :swagger-ui-error})))

(defn attach-ui [{:keys [swagger-ui-url swagger-doc-url index-data]}]
  (if swagger-ui-url
    {:index-data (map (fn [data]
                        (assoc data
                          :spec-path swagger-doc-url
                          :ui-base-path (str swagger-ui-url "#!") ;; TODO: move all this logic to FE??
                          :ui-api-path (str swagger-ui-url "#!" (:ui-api-path data))))
                      index-data)}
    {:index-data (map (fn [data]
                        (assoc data
                          :spec-path swagger-doc-url
                          :ui-base-path (str "render?url=" swagger-doc-url "#!") ;; TODO: move all this logic to FE??
                          :ui-api-path (str "render?url=" swagger-doc-url "#!" (:ui-api-path data))))
                      index-data)}))

;;;
;;; Gluing everything togheter
;;;

(defn pipe [& fns]
  (fn [data]
    (reduce (fn [data f]
              (let [new-data (merge data (f data))]
                (if (:error new-data)
                  (reduced new-data)
                  new-data)))
            data
            fns)))

(defn if-version [f-v1 f-v2]
  (fn [{swagger-doc :swagger-doc :as data}]
    (if (= 2 (some-> swagger-doc :swagger Double/parseDouble Math/floor int))
      (f-v2 data)
      (f-v1 data))))

(defn collect-swagger-data [json-client http-client pipeline-config services]
  (let [pipeline (pipe
                   (partial find-swagger-docs json-client)
                   (if-version (partial collect-additional-controllers-for-v1 json-client)
                               (constantly nil))
                   (if-version parser/index-data-for-v1
                               parser/index-data-for-v2)
                   (partial find-swagger-ui http-client)
                   attach-ui)]
    (map (comp pipeline
               #(assoc pipeline-config :service-url %))
         services)))

(defn glue [json-client http-client pipeline-config services]
  (let [swagger-data (collect-swagger-data json-client http-client pipeline-config services)]
    {:index-data (mapcat :index-data (remove :error swagger-data))
     :all-data   swagger-data}))

(defn log-unhandled-exceptions [f]
  (fn [& args]
    (try
      (apply f args)
      (catch Exception e (error e)))))                      ;; TODO: make error handling configurable

(defn schedule-indexing [fetching-function {:keys [http-config] :as config} indexer]
  (let [json-client (http/create (assoc http-config :as :json))
        http-client (http/create http-config)
        pipeline-config (select-keys config [:possible-swagger-doc-suffix :possible-swagger-ui-suffix])
        collect-data (partial glue json-client http-client pipeline-config)
        run-indexing (log-unhandled-exceptions (comp indexer collect-data fetching-function))]
    {:json-client       json-client
     :http-client       http-client
     :initial-load-task (future (run-indexing))
     :scheduled-task    (schedule/schedule                  ;; TODO: make schedule configurable
                          {:minute (range 0 60 5)} run-indexing)}))

(defn destroy-component [{:keys [scheduled-task json-client http-client initial-load-task]}]
    (when scheduled-task
      (future-cancel scheduled-task)
      (future-cancel initial-load-task)
      (http/destroy json-client)
      (http/destroy http-client)))