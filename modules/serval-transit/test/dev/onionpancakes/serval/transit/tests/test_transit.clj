(ns dev.onionpancakes.serval.transit.tests.test-transit
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty.test
             :refer [with-response send]]
            [dev.onionpancakes.serval.transit :as srv.transit]
            [clojure.test :refer [deftest is]]
            [cognitect.transit :as transit]))

(deftest test-transit-writable
  (let [value {:foo "bar"}
        body  (srv.transit/transit-writable value :json)]
    (with-response {:serval.response/body body}
      (let [result (-> (send nil :input-stream)
                       (:body)
                       (transit/reader :json)
                       (transit/read))]
        (is (= value result))))))
