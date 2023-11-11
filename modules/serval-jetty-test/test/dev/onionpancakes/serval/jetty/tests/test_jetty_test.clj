(ns dev.onionpancakes.serval.jetty.tests.test-jetty-test
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty.test
             :refer [with-handler with-response send]]
            [clojure.test :refer [deftest is]]))

(defn example-handler
  [ctx]
  {:serval.response/status 200
   :serval.response/body   "foo"})

(deftest test-with-handler
  (with-handler example-handler
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-with-response
  (with-response {:serval.response/status 200
                  :serval.response/body   "foo"}
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))
