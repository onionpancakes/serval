(ns dev.onionpancakes.serval.core.tests.test-core-handlers
  (:require [dev.onionpancakes.serval.core :as srv]
            [clojure.test :refer [deftest is]]))

(deftest test-headers
  (is (= (srv/headers {} {})
         {:serval.response/headers {}}))
  (is (= (srv/headers {} {"Foo" "Bar"})
         {:serval.response/headers {"Foo" "Bar"}})))

(deftest test-body
  (is (= (srv/body {} "Foo")
         {:serval.response/body "Foo"}))
  (is (= (srv/body {} "Foo" "text/plain")
         {:serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (srv/body {} "Foo" "text/plain" "utf-8")
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
  (let [ret (-> (srv/do-filter-chain {})
                (:serval.filter/do-filter-chain))]
    (is ret))
  (let [req                        (Object.)
        resp                       (Object.)
        [ret-req ret-resp :as ret] (-> (srv/do-filter-chain {} req resp)
                                       (:serval.filter/do-filter-chain))]
    (is (vector? ret))
    (is (identical? req ret-req))
    (is (identical? resp ret-resp))))
