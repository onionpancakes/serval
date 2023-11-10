(ns dev.onionpancakes.serval.jsonista.tests.test-jsonista
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty.test
             :refer [with-response send]]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn json-bytes
  [value]
  (let [out (ByteArrayOutputStream.)
        _   (json/write-value out value)]
    (.toByteArray out)))

(deftest test-read-json
  (let [value {"foo" "bar"}
        in    (ByteArrayInputStream. (json-bytes value))
        ctx   {:serval.context/request {:body in}}
        ret   (srv.json/read-json ctx)]
    (is (= (:serval.jsonista/value ret) value)))

  ;; Custom value key
  (let [value {"foo" "bar"}
        in    (ByteArrayInputStream. (json-bytes value))
        ctx   {:serval.context/request {:body in}}
        ret   (srv.json/read-json ctx {:value-key :value})]
    (is (= (:value ret) value)))

  ;; Keyword keys object mapper
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (json-bytes value))
        ctx   {:serval.context/request {:body in}}
        ret   (srv.json/read-json ctx {:object-mapper srv.json/keyword-keys-object-mapper})]
    (is (= (:serval.jsonista/value ret) value))))

(deftest test-read-json-error
  (let [in  (ByteArrayInputStream. (.getBytes "{foo"))
        ctx {:serval.context/request {:body in}}
        ret (srv.json/read-json ctx)]
    (is (:serval.jsonista/error ret)))

  ;; Custom error key
  (let [in  (ByteArrayInputStream. (.getBytes "{foo"))
        ctx {:serval.context/request {:body in}}
        ret (srv.json/read-json ctx {:error-key :error})]
    (is (:error ret))))

(deftest test-json-body
  (let [value {"foo" "bar"}]
    (with-response {:serval.response/body (srv.json/json-value value)}
      (let [resp       (send)
            resp-value (json/read-value (:body resp))]
        (is (= resp-value value))))))
