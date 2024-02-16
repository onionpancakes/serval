(ns dev.onionpancakes.serval.core.tests.test-response-body
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.response.body :as resp.body]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is are]])
  (:import [jakarta.servlet.http HttpServletResponse]
           [java.io ByteArrayInputStream File]
           [java.nio.file Path]))

(def ^java.net.URL example-url
  (io/resource "dev/onionpancakes/serval/core/tests/data/foo.txt"))

(deftest test-body
  (are [body expected] (with-handler (fn [_ _ ^HttpServletResponse response]
                                       (.setCharacterEncoding response "utf-8")
                                       (resp.body/write-body response body))
                         (= (:body (send)) expected))
    (.getBytes "foo" "UTF-8")                         "foo"
    "foo"                                             "foo"
    (File. (.toURI example-url))                      "foo"
    (ByteArrayInputStream. (.getBytes "foo" "UTF-8")) "foo"
    example-url                                       "foo"
    (Path/of (.toURI example-url))                    "foo"
    (eduction (map identity) ["foo" "bar"])           "foobar"
    `("foo" "bar")                                    "foobar"
    nil                                               ""))
