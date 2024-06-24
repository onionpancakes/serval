(ns dev.onionpancakes.serval.transit.tests.test-transit
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty-test
             :refer [with-handler send]]
            [dev.onionpancakes.serval.transit :as srv.transit]
            [clojure.test :refer [deftest is]]
            [cognitect.transit :as transit]))

(deftest test-transit-writable
  (with-handler (fn [_ _ response]
                  (let [body (srv.transit/transit-writable {"foo" "bar"} :json)]
                    (srv/write-body response body)))
    (let [resp   (send nil :input-stream)
          result (-> (:body resp)
                     (transit/reader :json)
                     (transit/read))]
      (is (= result {"foo" "bar"})))))
