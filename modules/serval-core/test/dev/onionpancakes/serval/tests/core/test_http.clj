(ns dev.onionpancakes.serval.tests.core.test-http
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.server
             :refer [with-handler with-response]]
            [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [clojure.test :refer [deftest is]])
  (:import [java.io ByteArrayInputStream]
           [java.util.concurrent CompletableFuture]))

(deftest test-body
  (with-response {:serval.response/body (.getBytes "foo")}
    (is (= (:body (send)) "foo")))
  (with-response {:serval.response/body "foo"}
    (is (= (:body (send)) "foo")))
  (with-response (let [body (ByteArrayInputStream. (.getBytes "foo"))]
                   {:serval.response/body body})
    (is (= (:body (send)) "foo")))
  (with-response {}
    (is (= (:body (send)) "")))
  (with-response (let [body (CompletableFuture/completedFuture "foo")]
                   {:serval.response/body body})
    (is (= (:body (send)) "foo"))))
