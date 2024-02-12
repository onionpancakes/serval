(ns dev.onionpancakes.serval.core.tests.test-response-http
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
  (with-response {:serval.response/headers {"str-header"     "foo"
                                            "date-header"    #inst "2000-01-01"
                                            "inst-header"    (.toInstant #inst "2000-01-01")
                                            "long-header"    1
                                            "indexed-header" ["foo" 1]
                                            "seqable-header" '("foo" 1)}}
    (let [{:strs [str-header
                  date-header
                  inst-header
                  long-header
                  indexed-header
                  seqable-header]} (:headers (send))]
      (is (= str-header ["foo"]))
      (is (= date-header ["Sat, 01 Jan 2000 00:00:00 GMT"]))
      (is (= inst-header ["Sat, 01 Jan 2000 00:00:00 GMT"]))
      (is (= long-header ["1"]))
      (is (= indexed-header ["foo" "1"]))
      (is (= seqable-header ["foo" "1"])))))

(def ^java.net.URL example-foo-url
  (io/resource "dev/onionpancakes/serval/core/tests/data/foo.txt"))

(deftest test-body
  (are [body expected] (with-response {:serval.response/body body}
                         (let [resp (send)]
                           (= (:status resp) 200)
                           (= (:body resp) expected)))
    (.getBytes "foo")                         "foo"
    "foo"                                     "foo"
    (File. (.toURI example-foo-url))          "foo"
    (ByteArrayInputStream. (.getBytes "foo")) "foo"
    example-foo-url                           "foo"
    (Path/of (.toURI example-foo-url))        "foo"
    (eduction (map identity) ["foo" "bar"])   "foobar"
    `("foo" "bar")                            "foobar"
    nil                                       ""))

(deftest test-body-encoding
  (are [body enc expected] (with-response {:serval.response/body body
                                           :serval.response/content-type "text/plain"
                                           :serval.response/character-encoding enc}
                             (= (:body (send)) expected))
    "foo"                      nil      "foo"
    "foo"                      "utf-8"  "foo"
    "foo"                      "utf-16" "foo"
    (.getBytes "foo" "utf-16") "utf-16" "foo"))

(deftest test-send-redirect
  (with-response {:serval.response/send-redirect "/"}
    (let [ret (send)]
      (is (= (:status ret) 302)))))

(deftest test-send-error
  (with-response {:serval.response/send-error 400}
    (let [ret (send)]
      (is (= (:status ret) 400))))
  (with-response {:serval.response/send-error [400 "message"]}
    (let [ret (send)]
      (is (= (:status ret) 400)))))
