(ns dev.onionpancakes.serval.jsonista.tests.test-jsonista
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]))

(deftest test-json-writable
  (with-handler (fn [_ _ response]
                  (let [body (srv.json/json-writable {"foo" "bar"})]
                    (srv/write-body response body)))
    (let [resp (send)]
      (is (= (json/read-value (:body resp)) {"foo" "bar"})))))
