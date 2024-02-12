(ns dev.onionpancakes.serval.core.tests.test-core-handlers
  (:require [dev.onionpancakes.serval.core :as srv]
            [clojure.test :refer [deftest is]]))

(deftest test-headers
  (is (= (srv/headers {} {})
         {:serval.response/headers {}}))
  (is (= (srv/headers {} {"Foo" "Bar"})
         {:serval.response/headers {"Foo" "Bar"}})))

(deftest test-body
  (is (= (srv/set-body {} "Foo")
         {:serval.response/body "Foo"}))
  (is (= (srv/set-body {} "Foo" "text/plain")
         {:serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (srv/set-body {} "Foo" "text/plain" "utf-8")
         {:serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"})))

(deftest test-response
  (is (= (srv/response {} 200)
         {:serval.response/status 200}))
  (is (= (srv/response {} 200 "Foo")
         {:serval.response/status 200
          :serval.response/body   "Foo"}))
  (is (= (srv/response {} 200 "Foo" "text/plain")
         {:serval.response/status       200
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (srv/response {} 200 "Foo" "text/plain" "utf-8")
         {:serval.response/status             200
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"}))
  (is (= (srv/response {:foo :bar} 200 "Foo")
         {:foo                    :bar
          :serval.response/status 200
          :serval.response/body   "Foo"}))
  (is (= (srv/response {} 400 "Foo" "text/plain")
         {:serval.response/status       400
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (srv/response {:foo :bar} 400 "Foo" "text/plain")
         {:foo                          :bar
          :serval.response/status       400
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (srv/response {} 400 "Foo" "text/plain" "utf-8")
         {:serval.response/status             400
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"}))
  (is (= (srv/response {:foo :bar} 400 "Foo" "text/plain" "utf-8")
         {:foo                                :bar
          :serval.response/status             400
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"})))

(deftest test-do-filter-chain
  (is (= (srv/do-filter-chain {})
         {:serval.filter/do-filter-chain true})))
