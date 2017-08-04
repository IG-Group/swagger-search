(ns com.ig.swagger.search.routes
  "Contains all the http endpoints, to be splitted if it becomes too big"
  (:require
    [ring.util.http-response :refer :all]
    [compojure.api.sweet :refer :all]
    [compojure.route :as route]
    [com.ig.swagger.search.index :as index]
    [com.ig.swagger.search.proxy :as proxy]
    [clojure.string :as str]
    [selmer.parser :refer [render-file]]
    [ring.util.response :refer [redirect]])
  (:import (org.apache.lucene.queryparser.classic ParseException)
           (org.apache.lucene.index IndexNotFoundException)))

;;;
;;; Http API
;;;

(defn sanitize-query [q]
  (let [alpha-numeric (str/replace q "/" "\\/")
        forward-slash (str/replace alpha-numeric "[^a-zA-Z 0-9]" "")]
    forward-slash))

(defn format-service-name [service-name]
  (str/capitalize (str/replace service-name "/" "")))

(defn get-filtered-services [index]
  (filter (fn [service]
            (not-empty (:servlet-context service)))
          (index/get-swagger-services index)))

(defn get-query-result [index query]
  (when query
    (merge {:query query}
           (try
             {:query-results (index/search index (sanitize-query query))}
             (catch ParseException _
               {:error "Invalid query submitted"})
             (catch IndexNotFoundException _
               {:error "Index is not available"})))))

(defn get-swagger-services [index]
  (map (fn [service]
         (assoc service
           :official-name (format-service-name (:servlet-context service))))
       (get-filtered-services index)))

(def http-api
  (routes
    (api
      (swagger-routes {:ui   "/swagger/ui/"
                       :spec "/swagger/swagger.json"
                       :data {:info {:title "Swagger Search API" :description "An API for finding swagger APIs across IG services"}}
                       :tags [{:name "API search", :description "operations for searching for swagger APIs"}]})

      (GET "/" {context :servlet-context-path}
        :no-doc true
        (redirect (str context "/search")))

      (GET "/search" {index :index context :servlet-context-path}
        :no-doc true
        :query-params [{query :- String nil}]
        (ok (render-file "swagger-search.html" (assoc (get-query-result index query)
                                                 :servlet-context context
                                                 :swagger-services (get-swagger-services index)))))

      (GET "/render" {context :servlet-context-path}
        :no-doc true
        :query-params [url :- String]
        (ok (render-file "swagger-ui.html" {:spec-url     url
                                            :proxy-prefix (str context "/api/proxy?url=")})))

      (GET "/api/search" {index :index}
        :tags ["API search"]
        :summary "Search for swagger API"
        :query-params [query :- String]
        (try
          (ok (index/search index (sanitize-query query)))
          (catch ParseException _
            (bad-request "Invalid query submitted"))
          (catch IndexNotFoundException _
            (service-unavailable "Index is not available"))))

      (GET "/parsing-result" {index :index}
        :tags ["debug"]
        :summary "Returns the error details for a particular service"
        :query-params [service-url :- String]
        (ok (index/parsing-result-for index service-url)))

      (ANY "/api/proxy" {client :proxy-client :as request}
        :no-doc true
        :query-params [url :- String]
        (select-keys (proxy/handle-proxy-request client request url) [:status :body :headers])))
    (route/resources "/")
    (route/not-found "<h1>Page not found</h1>")))


