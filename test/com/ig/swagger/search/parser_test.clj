(ns com.ig.swagger.search.parser-test
  (:require [com.ig.swagger.search.parser :as parser]
            [cheshire.core :as json])
  (:use
    [clojure.test :only [deftest]]
    [midje.sweet]))

(def file-dir "test/test_data/")

(defn get-json-document [file-name]
  (json/parse-string (slurp (clojure.java.io/file (str file-dir file-name))) true))

(defn parse-v2 [file]
  (:index-data (parser/index-data-for-v2 {:swagger-doc (get-json-document file)})))

(defn parse-v1 [file]
  (:index-data (parser/index-data-for-v1 {:v1-api-docs {"any" (get-json-document file)}})))

(deftest parse-swagger-response-v2
  (facts "parsing the swagger v2 response produces a result ready for lucene"
         (parse-v2 "parser_v2_working.json") =>
         (just [(contains {:ui-api-path             "/controller/operationId"
                           :path                    "/ping/abc"
                           :swagger-version         "2.0"
                           :summary-and-description "ping pong"
                           :method                  "GET"
                           :service-version         nil
                           :service-name            "A-tomcat-service"
                           :servlet-context         "/a-tomcat-service"})
                (contains {:ui-api-path             "/controller/operationId"
                           :path                    "/search"
                           :swagger-version         "2.0"
                           :summary-and-description "Search for API"
                           :method                  "GET"
                           :service-version         nil
                           :service-name            "A-tomcat-service"
                           :servlet-context         "/a-tomcat-service"})]
               :in-any-order)))

(deftest parse-swagger-response-v2-api-path
  (facts "parsing the swagger v2 response without operationId to make sure the api-path is still correct"
         (parse-v2 "parser_v2_no_operationId.json") =>
         (just [(contains {:ui-api-path             "/controller/GET_ping_abc"
                           :path                    "/ping/abc"
                           :swagger-version         "2.0"
                           :summary-and-description "ping pong"
                           :method                  "GET"
                           :service-version         nil
                           :service-name            "A-tomcat-service"
                           :servlet-context         "/a-tomcat-service"})
                (contains {:ui-api-path             "/controller/GET_search"
                           :path                    "/search"
                           :swagger-version         "2.0"
                           :summary-and-description "Search for API"
                           :method                  "GET"
                           :service-version         nil
                           :service-name            "A-tomcat-service"
                           :servlet-context         "/a-tomcat-service"})]
               :in-any-order)))

(deftest parse-swagger-response-v2-api-path
  (facts "parsing the swagger v2 response with only path and method"
         (parse-v2 "parser_v2_minimum_fields.json") =>
         (just [(contains {:path                    "/ping/abc"
                           :method                  "GET"
                           :service-name            nil,
                           :service-version         nil,
                           :swagger-version         "2.0"
                           :servlet-context         nil
                           :summary-and-description ""
                           :responses               []
                           :parameters              []
                           :ui-api-path             "/default/GET_ping_abc"})]
               :in-any-order)))

(deftest parse-swagger-response-v1
  (facts "parsing the swagger v1 response produces a result ready for lucene"
         (parse-v1 "parser_v1_working.json") =>
         (just [{:path            "/ping/abc"
                 :swagger-version "1.2"
                 :summary         "ping pong"
                 :method          "GET"
                 :ui-api-path     "/somebasepath/operationPing"
                 :service-name    "A-tomcat-service",
                 :servlet-context "/a-tomcat-service"}
                {:path            "/search"
                 :swagger-version "1.2"
                 :summary         "Search for API"
                 :method          "GET"
                 :ui-api-path     "/somebasepath/operationSearch"
                 :service-name    "A-tomcat-service",
                 :servlet-context "/a-tomcat-service"}]
               :in-any-order)))

(deftest circular-refs
         (facts "works"
                (->
                  {:definitions
                   {:a
                    {:type        "object",
                     :description "a",
                     :properties  {:child {:$ref "#/definitions/b"}}}
                    :b
                    {:type       "object",
                     :properties {:child {:$ref "#/definitions/c"}}}
                    :c
                    {:type       "object",
                     :properties {:child {:$ref "#/definitions/a"}}}}}
                  parser/resolve-refs
                  (get-in [:definitions :a
                           :properties :child
                           :properties :child
                           :properties :child
                           :description]))
                => "a"

                (->
                  {:definitions
                   {:a
                    {:type        "object",
                     :description "a",
                     :properties  {:id       {:type "integer"},
                                   :name     {:type "string", :description "Category name"},
                                   :children {:type  "array",
                                              :items {:$ref "#/definitions/b"}}
                                   :parent   {:$ref "#/definitions/a"}}}
                    :b
                    {:type        "object",
                     :description "b",
                     :properties  {:id       {:type "integer"},
                                   :name     {:type "string", :description "the other name"},
                                   :children {:type  "array",
                                              :items {:$ref "#/definitions/a"}}}}}}
                  parser/resolve-refs
                  (get-in [:definitions :a
                           :properties :children :items
                           :properties :children :items
                           :description
                           ]))
                => "a"
                ))
