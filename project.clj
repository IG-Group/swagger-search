(defproject ig/swagger-search "0.1.0"
  :description "An application that collects and indexes swagger docs from your microservices architecture"
  :url "https://github.com/IG-Group/swagger-search"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/tools.logging "0.3.1"]
                 [org.clojure/clojure "1.8.0"]
                 [zclucy "0.9.2"]
                 [medley "0.7.0"]
                 [http.async.client "1.1.0"]
                 [metosin/compojure-api "1.0.1" :exclusions [org.eclipse.jetty/jetty-server]]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [ring/ring-servlet "1.4.0" :exclusions [javax.servlet/servlet-api]]
                 [ring/ring-core "1.4.0"]
                 [metosin/ring-swagger-ui "2.1.8-M1"]
                 [selmer "1.0.4"]
                 [schejulure "1.0.1"]
                 ;[ch.qos.logback/logback-classic nil]
                 ;[org.slf4j/slf4j-api nil]
                 ; [org.slf4j/log4j-over-slf4j nil]
                 ;[org.slf4j/jcl-over-slf4j nil]
                 [org.tcrawley/dynapath "0.2.4"]]
  :profiles {:dev     {:dependencies [[ring/ring-mock "0.3.0"]
                                      [midje "1.8.3"]]}
             :uberjar {:dependencies [[ring/ring-jetty-adapter "1.5.0"]]
                       :aot :all
                       :omit-source true}})
