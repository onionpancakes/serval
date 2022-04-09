(ns dev.onionpancakes.serval.tests.core.test-io-body
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.server
             :refer [with-handler]]
            [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [clojure.test :refer [deftest is]]))

(deftest test-service-body
  (with-handler (fn [ctx]
                  {:serval.response/body "foo"})
    (is (= (:body (send nil)) "foo"))))


