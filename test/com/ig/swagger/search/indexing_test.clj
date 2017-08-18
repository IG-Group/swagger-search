(ns com.ig.swagger.search.indexing-test
  (:require [com.ig.swagger.search.parser-test :as test-util]
            [com.ig.swagger.search.index :as index])
  (:use
    [clojure.test :only [deftest]]
    [midje.sweet]))


(defn parse-and-index [file]
  (let [endpoints (test-util/parse-v2 file)]
    (index/build-index endpoints)))

(let [index (parse-and-index "v2_big_example.json")]

  (deftest index-and-search []
    (facts "ui path is not indexed but it is stored"
           (index/search* index "post_minus" 10) => []

           (index/search* index "minus" 10)
           =>
           (just [(contains {:ui-api-path "/math/post_minus"})])))
  )

(comment

  (->>
    (index/search*
      (parse-and-index "v2_big_example.json")
      "method:get" 10)
    ))