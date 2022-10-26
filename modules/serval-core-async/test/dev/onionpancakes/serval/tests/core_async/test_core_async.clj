(ns dev.onionpancakes.serval.tests.core-async.test-core-async
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core-async :as srv.async]
            [dev.onionpancakes.serval.test-utils.server
             :refer [with-response]]
            [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [clojure.test :refer [deftest is]]
            [clojure.core.async :as async]))

(deftest test-channel-body
  (let [body (-> (async/to-chan! ["foo" "bar" "baz"])
                 (srv.async/channel-body))]
    (with-response {:serval.response/body body}
      (is (= (:body (send)) "foobarbaz")))))
