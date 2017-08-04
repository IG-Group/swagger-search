(ns com.ig.swagger.search.http-client.core
  (:require [http.async.client :as http]
            [http.async.client.request :as http-req]
            [clojure.tools.logging :refer [debug info]]
            [cheshire.core :as json])
  (:import (com.ning.http.client Request)))

(set! *warn-on-reflection* true)

(defn ->json
  "Adds appropriate JSON headers and parses the body to a keyword keyed edn"
  [handler]
  (fn [req]
    (let [resp (handler
                 (-> req
                     (update-in [:headers] merge {"Accept"       "application/json"
                                                  "Content-Type" "application/json"})
                     (update-in [:body] #(when % (json/generate-string %)))))]
      (try
        (update-in resp [:body] json/parse-string true)
        (catch Exception e (str "Error in parsing the JSON file"))))))

(defn ->simpler-api
  [handler]
  (fn
    ([req] (handler req))
    ([method url & {:as params}]
     (let [request {:method                           method
                    :url                              url
                    (if (= :get method) :query :body) (dissoc params :q :b :h)}
           assoc-if (fn [m src dest]
                      (if-let [to-assoc (get params src)]
                        (assoc m dest to-assoc)
                        m))]
       (:body
         (handler (-> request
                      (assoc-if :h :headers)
                      (assoc-if :q :query)
                      (assoc-if :b :body))))))))

(defn logger [handler]
  (fn [request]
    (let [response (handler request)]                       ; http-fn gets executed here
      (debug "request=" request ", response=" response)
      response)))

(defn- http-fn [client]
  (fn [{:keys [method url] :as req}]
    (let [request ^Request (apply http-req/prepare-request method url (apply concat (dissoc req :method :url)))
          _ (info (str "requestUrl=" (.getUrl request)))
          response (http/await (http-req/execute-request client request))]
      (assoc response
        :status (http/status response)
        :body (http/string response)
        :error (http/error response)
        :headers (http/headers response)))))

(defn create-client [{:keys [connection-timeout request-timeout max-connections] :as config}]
  {:pre [connection-timeout request-timeout max-connections]}
  (let [client (http/create-client
                 :connection-timeout connection-timeout :request-timeout request-timeout
                 :max-conns-per-host max-connections :max-conns-total max-connections
                 :idle-in-pool-timeout 60000)]
    (with-meta (logger (http-fn client))
               {:client client})))

(defn create [config]
  (let [base (create-client (select-keys config [:connection-timeout :request-timeout :max-connections]))
        configured base
        configured (case (:as config)
                     :json (->json configured)
                     configured)]
    (with-meta
      (->simpler-api configured)
      (merge config {:base base}))))

(defn- base-destroy [client]
  (http/close (:client (meta client))))

(defn destroy [http-client]
  (base-destroy (:base (meta http-client))))

(defmacro with-client [[client-name config] & body]
  `(let [~client-name (create ~config)]
     (try
       ~@body
       (finally (destroy ~client-name)))))
