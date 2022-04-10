(ns dev.onionpancakes.serval.tests.core.test-http-response
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.server
             :refer [with-handler with-response]]
            [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [clojure.test :refer [deftest is are]])
  (:import [java.io ByteArrayInputStream]
           [java.util.concurrent CompletableFuture]))

(deftest test-status
  (are [status] (with-response {:serval.response/status status}
                  (= (:status (send)) status))
    200
    300
    400
    500))

(deftest test-headers
  (with-response {:serval.response/headers {"foo" ["foo"]
                                            "bar" ["bar1" "bar2"]}}
    (let [{:strs [foo bar]} (:headers (send))]
      (is (= foo ["foo"]))
      (is (= bar ["bar1" "bar2"])))))

(deftest test-body
  (are [body expected] (with-response {:serval.response/body body}
                         (= (:body (send)) expected))
    (.getBytes "foo")                         "foo"
    "foo"                                     "foo"
    (ByteArrayInputStream. (.getBytes "foo")) "foo"
    nil                                       ""
    (CompletableFuture/completedFuture "foo") "foo"))

(deftest test-body-encoding
  (are [body enc expected] (with-response {:serval.response/body body
                                           :serval.response/content-type "text/plain"
                                           :serval.response/character-encoding enc}
                             (= (:body (send)) expected))
    "foo" nil      "foo"
    "foo" "utf-8"  "foo"
    "foo" "utf-16" "foo"

    (.getBytes "foo" "utf-16") "utf-16" "foo"))

;; TODO: test cookies?

(deftest test-complete-async
  ;; Normal async.
  (with-handler (fn [ctx]
                  (.startAsync (:serval.service/request ctx))
                  {:serval.response/status 200
                   :serval.response/body   "foo"})
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo"))))
  ;; Throw an error while in async mode.
  (with-handler (fn [ctx]
                  (.startAsync (:serval.service/request ctx))
                  (throw (ex-info "Uhoh" {}))
                  {:serval.response/status 200
                   :serval.response/body   "foo"})
    (let [resp (send)]
      (is (= (:status resp) 500)))))
