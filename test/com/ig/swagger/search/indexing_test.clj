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
           (just [(contains {:ui-api-path "/math/post_minus"})]))

    (facts "can search on the parameter names"
           (index/search* index "parameters:NewSingleToppingPizza" 10)
           =>
           (just [(contains {:path "/pizza" :method "post"})])

           (index/search* index "NewSingleToppingPizza" 10)
           =>
           (just [(contains {:path "/pizza" :method "post"})
                  (contains {:path "/pizza" :method "get"})]))

    (facts "can search on the property names of the params"

           (fact "inlined object definition"
                 (index/search* index "propertyInsideParamDef" 10)
                 =>
                 (just [(contains {:path "/minus" :method "post"})]))

           (fact "reference"
                 (index/search* index "toppings" 10)
                 =>
                 (contains [(contains {:path "/pizza" :method "post"})]))

           (fact "property within array"
                 (index/search* index "propertyInsideObjectInsideArray" 10)
                 =>
                 (contains [(contains {:path "/anonymous" :method "put"})]))

           (fact "property within an object within an object"
                 (index/search* index "innerInnerField" 10)
                 =>
                 (just [(contains {:path "/pizzas" :method "post"})])))

    (facts "can search on the responses"

           (index/search* index "total" 10)
           =>
           (contains [(contains {:path "/minus" :method "post"})])

           (index/search* index "responses:total" 10)
           =>
           (contains [(contains {:path "/minus" :method "post"})]))
    )
  )

(let [index (parse-and-index "youtube.json")]
  (deftest youtube []
    (facts "can search types"
           (index/search* index "types:FanFundingEventSnippet" 10) =not=> []
           (index/search* index "FanFundingEventSnippet" 10) =not=> [])

    (facts "path item parameters object"
           (index/search* index "method:parameters" 10) => []

           (index/search* index "prettyPrint" 10) =not=> []

           (index/search* index "fan funding events" 10) =not=> []

           (index/search* index "fan funding event" 10) =not=> []

           )))


(let [index (parse-and-index "swagger-describing-json-schema.json")]
  (deftest schema-in-schema []
    (facts "it works"
           (index/search* index "responses:ref" 10) =not=> []
           (index/search* index "types:JsonSchema" 10) =not=> [])))

(comment

  (youtube)
  (index-and-search)
  (->>
    (index/search*
      (parse-and-index "youtube.json")
      "types:FanFundingEventSnippet" 10)
    )

  (index/search*
    (parse-and-index "youtube.json")
    "method:put The part parameter specifies a comma-separated list of commentThread resource properties that " 10)

  (index/search*
    (parse-and-index "magento.json")
    "catalog-data-category-tree-interface" 10)

  (->>
    (index/search*
      (parse-and-index "v2_big_example.json")
      "innerInnerField" 10)
    ))