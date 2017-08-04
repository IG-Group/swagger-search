(ns com.ig.swagger.search.standalone
  (:require [com.ig.swagger.search.core :as search]
            [ring.adapter.jetty :as jetty]
            [clojure.java.io :as io]
            [dynapath.util :as dp]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn])
  (:gen-class))

(defonce the-system (atom nil))

(defn start [{join? :join?} config]
  (let [system (search/system-start config)]
    (reset! the-system (assoc system
                         :jetty (jetty/run-jetty (:routes system) {:port  3000
                                                                   :join? join?})))))

(defn stop []
  (when-let [system @the-system]
    (.stop (:jetty system))
    (search/system-stop system)))

(defn add-to-classpath [file-or-url]
  (log/info "Adding" (.getAbsolutePath (io/as-file file-or-url)) "to classpath")
  (dp/add-classpath-url (.getContextClassLoader (Thread/currentThread)) (io/as-url file-or-url)))

(defn add-libs-to-classpath [home-dir]
  (let [lib-dir (io/file (io/as-file home-dir) "libs")]
    (doseq [file (file-seq lib-dir)]
      (when (or (.isDirectory file)
                (.endsWith (.getName file) "jar"))
        (add-to-classpath file)))))

(defn -main [& args]
  (let [home-dir (or
                   (cond-> (or (System/getenv "SWAGGER_HOME") (System/getProperty "SWAGGER_HOME") (first args))
                           some? io/file
                           #(.isDirectory %) io/as-url)
                   (throw (RuntimeException. "No SWAGGER_HOME environment set. Please see documentation")))
        _ (add-to-classpath home-dir)

        config (or
                 (some-> (io/resource "swagger.config.edn")
                         io/file
                         slurp
                         edn/read-string)
                 (throw (RuntimeException. "No swagger.config.edn file at $SWAGGER_HOME")))]
    (add-libs-to-classpath home-dir)
    (start {:join? true} config)))

(comment
  (start {:join? false}
         {;read from file?
          })
  (stop)
  )