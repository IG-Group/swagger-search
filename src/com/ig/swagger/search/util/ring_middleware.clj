(ns com.ig.swagger.search.util.ring-middleware)

(defn remove-context
  "Removes the deployed servlet context from a URI when running as a
   deployed web application"
  [handler]
  (fn [request]
    (if-let [context (:servlet-context-path request)]
      (let [uri (:uri request)]
        (if (.startsWith uri context)
          (handler (assoc request :uri
                                  (.substring uri (.length context))))
          (handler request)))
      (handler request))))

(defn wrap-with-additional-keys-in-req
  "Adds to all requests the key value pairs"
  [handler & additional-keys-in-req]
  (fn [req]
    (handler (apply assoc req additional-keys-in-req))))
