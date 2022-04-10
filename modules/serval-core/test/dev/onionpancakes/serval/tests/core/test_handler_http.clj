(ns dev.onionpancakes.serval.tests.core.test-handler-http
  (:require [dev.onionpancakes.serval.handler.http :as h]
            [clojure.test :refer [deftest is]]))

(deftest test-response-handler
  (is (= (h/response {} 200 "Foo")
         {:serval.response/status 200
          :serval.response/body   "Foo"}))
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

#_(deftest test-set-headers
  (is (= (h/set-headers nil nil)
         {:serval.response/headers {}}))
  (is (= (h/set-headers {} {})
         {:serval.response/headers {}}))
  (is (= (h/set-headers {:foo :bar} {})
         {:foo                     :bar
          :serval.response/headers {}}))
  (is (= (h/set-headers {:foo :bar} {"Foo" ["Bar"]})
         {:foo                     :bar
          :serval.response/headers {"Foo" ["Bar"]}}))
  (is (= (h/set-headers {:foo                     :bar
                         :serval.response/headers {"Foo" ["Bar"], "ABC" ["123"]}}
                        {"Foo" ["Baz"] "XYZ" [456]})
         {:foo                     :bar
          :serval.response/headers {"Foo" ["Baz"] "ABC" ["123"] "XYZ" [456]}})))
