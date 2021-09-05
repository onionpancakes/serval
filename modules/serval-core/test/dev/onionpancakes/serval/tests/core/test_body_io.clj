(ns dev.onionpancakes.serval.tests.core.test-body-io
  (:require [dev.onionpancakes.serval.io.body :as b]
            [dev.onionpancakes.serval.mock :as mock]
            [clojure.test :refer [deftest is]])
  (:import [java.io ByteArrayInputStream]))

(deftest test-not-async-types
  (is (not (b/async-body? (byte-array 0) nil)))
  (is (not (b/async-body? "Foo" nil)))
  (is (not (b/async-body? (proxy [java.io.InputStream] []) nil)))
  (is (not (b/async-body? nil nil))))

(deftest test-sync-writes

  ;; Bytes
  (let [resp    (mock/mock-http-servlet-response)
        ctx     {:serval.service/response resp}
        _       (b/write-body (.getBytes "Foobar" "utf-8") ctx)
        written (.toByteArray (:output-stream resp))]
    (is (= "Foobar" (String. written "utf-8"))))

  ;; String
  (let [resp (mock/mock-http-servlet-response)
        ctx  {:serval.service/response resp}
        _    (b/write-body "Foobar" ctx)]
    (is (= "Foobar" (str (:writer resp)))))

  ;; InputStream
  (let [resp    (mock/mock-http-servlet-response)
        ctx     {:serval.service/response resp}
        in      (ByteArrayInputStream. (.getBytes "Foobar" "utf-8"))
        _       (b/write-body in ctx)
        written (.toByteArray (:output-stream resp))]
    (is (= "Foobar" (String. written "utf-8"))))

  ;; nil
  (let [resp (mock/mock-http-servlet-response)
        ctx  {:serval.service/response resp}
        _    (b/write-body nil ctx)]
    (is (== 0 (alength (.toByteArray (:output-stream resp)))))
    (is (= "" (str (:writer resp))))))

(deftest test-async-read
  ;; Read bytes
  (let [req (mock/mock-http-servlet-request-string {} "Foobar" "utf-8")
        res (.get (b/read-body-as-bytes-async! req))]
    (is (= "Foobar" (String. res "utf-8"))))

  ;; Read string
  (let [req (mock/mock-http-servlet-request-string {} "Foobar" "utf-8")
        res (.get (b/read-body-as-string-async! req))]
    (is (= "Foobar" res)))
  
  ;; Should fail without encoding set on request.
  (let [req (mock/mock-http-servlet-request-string {} "やばい" "utf-16")
        res (.get (b/read-body-as-string-async! req))]
    (is (not= "やばい" res)))
  
  ;; Works with encoding set.
  (let [req (mock/mock-http-servlet-request-string {:character-encoding "utf-16"}
                                                   "やばい" "utf-16")
        res (.get (b/read-body-as-string-async! req))]
    (is (= "やばい" res)))
a
  ;; Should fail when async not supported.
  (let [req (mock/mock-http-servlet-request-string {:async-supported? false}
                                                   "Foobar" "utf-8")]
    (is (thrown? IllegalStateException (b/read-body-as-string-async! req)))))

(defn run-tests []
  (clojure.test/run-tests 'dev.onionpancakes.serval.tests.core.test-body-io))
