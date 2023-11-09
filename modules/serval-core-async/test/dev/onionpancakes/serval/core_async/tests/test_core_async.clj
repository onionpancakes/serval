(ns dev.onionpancakes.serval.core-async.tests.test-core-async
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core-async :as srv.async]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-response send]]
            [clojure.test :refer [deftest is]]
            [clojure.core.async :as async]))

(deftest test-channel-body
  (let [body (-> (async/to-chan! ["foo" "bar" "baz"])
                 (srv.async/channel-body))]
    (with-response {:serval.response/body body}
      (is (= (:body (send)) "foobarbaz")))))
