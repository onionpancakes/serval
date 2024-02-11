(ns dev.onionpancakes.serval.jsonista.tests.test-jsonista
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty.test
             :refer [with-response send]]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]))

(deftest test-json-writable
  (let [value {"foo" "bar"}
        body  (srv.json/json-writable value)]
    (with-response {:serval.response/body body}
      (let [result (-> (send)
                       (:body)
                       (json/read-value))]
        (is (= value result))))))
