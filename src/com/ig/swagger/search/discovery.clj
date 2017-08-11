(ns com.ig.swagger.search.discovery)

(defn resolve-and-create [config f-name]
  (try
    (require (symbol (namespace f-name)))
    (catch Exception e (throw (RuntimeException. (str "Could not load namespace " (namespace f-name)) e))))
  (if-let [f (resolve f-name)]
    (f config)
    (throw (RuntimeException. (str "Could not find function " f-name)))))

(defn create-if-symbol [config f-or-fname]
  (if (fn? f-or-fname)
    f-or-fname
    (resolve-and-create config f-or-fname)))

(defn provider [{:keys [discovery-providers] :as config}]
  (let [build-in-providers (keep identity
                                 [(if (:uri-or-file config)
                                    'com.ig.swagger.search.discovery.providers.file/from-uri-or-file)
                                  (if (:classpath-file config)
                                    'com.ig.swagger.search.discovery.providers.file/from-classpath)
                                  (if (:server-list config)
                                    'com.ig.swagger.search.discovery.providers.file/server-list)])
        sources (map (partial create-if-symbol config)
                     (concat discovery-providers
                             build-in-providers))]
    (assert (seq sources) "No discovery providers configured. Please check your configuration")
    (fn []
      (distinct (mapcat #(%) sources)))))