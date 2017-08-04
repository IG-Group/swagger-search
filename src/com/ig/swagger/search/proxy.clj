(ns com.ig.swagger.search.proxy
  (:require
    [ring.util.http-response :refer :all]
    [compojure.api.sweet :refer :all]
    [selmer.parser :refer [render-file]]
    [ring.util.response :refer [redirect]]))

(defn- build-request [{:keys [request-method headers body]} endpoint]
  (let [proxy-request {:url     endpoint
                       :method  request-method
                       :headers (select-keys headers ["accept" "content-type" "content-length"])}]
    (if (#{:get :head} request-method)
      proxy-request
      (assoc proxy-request
        :body (slurp body)))))

(defn- stringify-header [[header-key header-value]]
  [(name header-key) header-value])

(defn- create-response-headers [headers]
  (into {} (map (partial stringify-header)
                (select-keys headers [:content-type :date]))))

(defn handle-proxy-request [client request endpoint]
  (-> (client (build-request request endpoint))
      (update :status :code)
      (update :headers create-response-headers)))