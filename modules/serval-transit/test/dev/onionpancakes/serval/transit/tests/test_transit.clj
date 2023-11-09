(ns dev.onionpancakes.serval.transit.tests.test-transit
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty.test
             :refer [with-response send]]
            [dev.onionpancakes.serval.transit :as srv.transit]
            [clojure.test :refer [deftest is]]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn transit-bytes
  [value type]
  (let [out    (ByteArrayOutputStream.)
        writer (transit/writer out type)
        _      (transit/write writer value)]
    (.toByteArray out)))

(deftest test-read-transit-type
  ;; Json
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :json))
        ctx   {:serval.service/request {:body in}}
        ret   (srv.transit/read-transit ctx :json)]
    (is (= (:serval.transit/value ret) value)))

    ;; Msgpack
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :msgpack))
        ctx   {:serval.service/request {:body in}}
        ret   (srv.transit/read-transit ctx :msgpack)]
    (is (= (:serval.transit/value ret) value))))

(deftest test-read-transit-reader-opts
  ;; Reader opts
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :json))
        ctx   {:serval.service/request {:body in}}
        ret   (srv.transit/read-transit ctx :json {:reader-opts {}})]
    (is (= (:serval.transit/value ret) value))))

(deftest test-read-transit-value
  ;; Default key
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :json))
        ctx   {:serval.service/request {:body in}}
        ret   (srv.transit/read-transit ctx :json)]
    (is (= (:serval.transit/value ret) value)))
  ;; Custom key
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :json))
        ctx   {:serval.service/request {:body in}}
        ret   (srv.transit/read-transit ctx :json {:value-key :body})]
    (is (= (:body ret) value))))

(deftest test-read-transit-error
  ;; Default key
  (let [in    (ByteArrayInputStream. (.getBytes "foo"))
        ctx   {:serval.service/request {:body in}}
        ret   (srv.transit/read-transit ctx :json)]
    (is (:serval.transit/error ret)))
  ;; Custom key
  (let [in    (ByteArrayInputStream. (.getBytes "foo"))
        ctx   {:serval.service/request {:body in}}
        ret   (srv.transit/read-transit ctx :json {:error-key :error})]
    (is (:error ret))))

(deftest test-transit-body
  (let [value {:foo "bar"}]
    (with-response {:serval.response/body (srv.transit/transit-value value :json)}
      (let [resp       (send nil :input-stream)
            reader     (transit/reader (:body resp) :json)
            read-value (transit/read reader)]
        (is (= read-value value))))))
