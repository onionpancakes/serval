(ns dev.onionpancakes.serval.tests.core-async.test-core-async
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core-async
             :refer [extend-channel-as-response-body!
                     channel-body]]
            [dev.onionpancakes.serval.test-utils.server
             :refer [with-response]]
            [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [clojure.test :refer [deftest is]]
            [clojure.core.async :refer [go chan close! >!]]))

(extend-channel-as-response-body!)

(deftest test-async-writables
  (with-response {:serval.response/body (go "foo")}
    (is (= (:body (send)) "foo")))
  (with-response {:serval.response/body (go (.getBytes "foo"))}
    (is (= (:body (send)) "foo"))))

(defn test-chan []
  (let [ch (chan)]
    (go
      (doseq [v ["foo" "bar" "baz"]]
        (>! ch v))
      (close! ch))
    ch))

(deftest test-channel-response-body
  (with-response {:serval.response/body (test-chan)}
    (is (= (:body (send)) "foobarbaz")))
  (with-response {:serval.response/body (channel-body (test-chan))}
    (is (= (:body (send)) "foobarbaz"))))
