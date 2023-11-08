(ns dev.onionpancakes.serval.core.tests.test-handlers-http
  (:require [dev.onionpancakes.serval.handlers.http :as h]
            [clojure.test :refer [deftest is]]))

(deftest test-response-handler
  (is (= (h/response {} 200)
         {:serval.response/status 200}))
  (is (= (h/response {} 200 "Foo")
         {:serval.response/status 200
          :serval.response/body   "Foo"}))
  (is (= (h/response {} 200 "Foo" "text/plain")
         {:serval.response/status       200
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (h/response {} 200 "Foo" "text/plain" "utf-8")
         {:serval.response/status             200
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"}))
  (is (= (h/response {:foo :bar} 200 "Foo")
         {:foo                    :bar
          :serval.response/status 200
          :serval.response/body   "Foo"}))
  (is (= (h/response {} 400 "Foo" "text/plain")
         {:serval.response/status       400
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (h/response {:foo :bar} 400 "Foo" "text/plain")
         {:foo                          :bar
          :serval.response/status       400
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (h/response {} 400 "Foo" "text/plain" "utf-8")
         {:serval.response/status             400
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"}))
  (is (= (h/response {:foo :bar} 400 "Foo" "text/plain" "utf-8")
         {:foo                                :bar
          :serval.response/status             400
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"})))

(deftest test-set-headers-handler
  (is (= (h/set-headers {} {"Foo" ["Bar"]})
         {:serval.response/headers {"Foo" ["Bar"]}})))
