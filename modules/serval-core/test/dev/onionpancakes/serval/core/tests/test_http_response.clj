(ns dev.onionpancakes.serval.core.tests.test-http-response
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty.test
             :refer [with-response send]]
            [clojure.test :refer [deftest is are]]
            [clojure.java.io :as io])
  (:import [java.io ByteArrayInputStream File]
           [java.nio.file Path]
           [java.util.concurrent CompletableFuture]))

(deftest test-status
  (are [status] (with-response {:serval.response/status status}
                  (= (:status (send)) status))
    200
    400))

(deftest test-headers
  (with-response {:serval.response/headers {"str-header"  ["foo"]
                                            "int-header"  [(int 1)]
                                            "date-header" [#inst "2000-01-01"]
                                            "inst-header" [(.toInstant #inst "2000-01-01")]
                                            "long-header" [1]}}
    (let [{:strs [str-header
                  int-header
                  date-header
                  inst-header
                  long-header]} (:headers (send))]
      (is (= str-header ["foo"]))
      (is (= int-header ["1"]))
      (is (= date-header ["Sat, 01 Jan 2000 00:00:00 GMT"]))
      (is (= inst-header ["Sat, 01 Jan 2000 00:00:00 GMT"]))
      (is (= long-header ["1"])))))

(def ^java.net.URL example-foo-url
  (io/resource "dev/onionpancakes/serval/core/tests/data/foo.txt"))

(deftest test-body
  (are [body expected] (with-response {:serval.response/body body}
                         (= (:body (send)) expected))
    (.getBytes "foo")                         "foo"
    "foo"                                     "foo"
    (File. (.toURI example-foo-url))          "foo"
    (ByteArrayInputStream. (.getBytes "foo")) "foo"
    example-foo-url                           "foo"
    (Path/of (.toURI example-foo-url))        "foo"
    (eduction (map identity) ["foo" "bar"])   "foobar"
    `("foo" "bar")                            "foobar"
    `("foo" ~(.getBytes "bar"))               "foobar"
    nil                                       ""))

(deftest test-body-throwable
  (with-response {:serval.response/body (ex-info "foo" {})}
    (let [body (:body (send))]
      (is (and (string? body) (not (empty? body)))))))

(deftest test-body-async
  (are [body expected] (with-response {:serval.response/body body}
                         (= (:body (send)) expected))
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

