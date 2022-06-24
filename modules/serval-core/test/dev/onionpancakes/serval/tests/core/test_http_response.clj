(ns dev.onionpancakes.serval.tests.core.test-http-response
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.server
             :refer [with-handler with-response]]
            [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.serval.core :as srv])
  (:import [jakarta.servlet ServletRequest]
           [java.io ByteArrayInputStream File]
           [java.nio ByteBuffer]
           [java.nio.file Path]
           [java.util ArrayList Collection]
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

(def ^java.net.URL example-foo
  (clojure.java.io/resource "dev/onionpancakes/serval/test_utils/example_foo.txt"))

(deftest test-body
  (are [body expected] (with-response {:serval.response/body body}
                         (= (:body (send)) expected))
    (.getBytes "foo")                         "foo"
    "foo"                                     "foo"
    (ByteArrayInputStream. (.getBytes "foo")) "foo"
    (Path/of (.toURI example-foo))            "foo"
    (File. (.toURI example-foo))              "foo"
    `("foo" "bar")                            "foobar"
    `("foo" ~(.getBytes "bar"))               "foobar"
    nil                                       ""
    (CompletableFuture/completedFuture "foo") "foo"

    ;; Async
    (srv/async-body (.getBytes "foo"))                   "foo"
    (srv/async-body "foo")                               "foo"
    (srv/async-body (ByteBuffer/wrap (.getBytes "foo"))) "foo"
    (let [coll [(ByteBuffer/wrap (.getBytes "foo"))]]
      (srv/async-body (ArrayList. ^Collection coll)))    "foo"))

(deftest test-body-encoding
  (are [body enc expected] (with-response {:serval.response/body body
                                           :serval.response/content-type "text/plain"
                                           :serval.response/character-encoding enc}
                             (= (:body (send)) expected))
    "foo" nil      "foo"
    "foo" "utf-8"  "foo"
    "foo" "utf-16" "foo"

    (.getBytes "foo" "utf-16") "utf-16" "foo"))

;; Todo: test cookies?

(deftest test-complete-async
  ;; Normal async.
  (with-handler (fn [ctx]
                  (.startAsync ^ServletRequest (:serval.service/request ctx))
                  {:serval.response/status 200
                   :serval.response/body   "foo"})
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo"))))
  ;; Throw an error while in async mode.
  (with-handler (fn [ctx]
                  (.startAsync ^ServletRequest (:serval.service/request ctx))
                  (throw (ex-info "Uhoh" {}))
                  {:serval.response/status 200
                   :serval.response/body   "foo"})
    (let [resp (send)]
      (is (= (:status resp) 500)))))
