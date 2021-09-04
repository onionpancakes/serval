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

(defn run-tests []
  (clojure.test/run-tests 'dev.onionpancakes.serval.tests.core.test-body-io))
