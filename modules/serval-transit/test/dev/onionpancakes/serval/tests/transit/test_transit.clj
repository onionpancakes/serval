(ns dev.onionpancakes.serval.tests.transit.test-transit
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [dev.onionpancakes.serval.test-utils.server
             :refer [with-response]]
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

(deftest test-read-transit
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :json))
        ctx   {:serval.service/request {:input-stream in}}
        ret   (srv.transit/read-transit ctx)]
    (is (= (:serval.transit/value ret) value)))

  ;; Empty options
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :json))
        ctx   {:serval.service/request {:input-stream in}}
        ret   (srv.transit/read-transit ctx {})]
    (is (= (:serval.transit/value ret) value)))

  ;; Json-verbose
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :json-verbose))
        ctx   {:serval.service/request {:input-stream in}}
        ret   (srv.transit/read-transit ctx {:type :json-verbose})]
    (is (= (:serval.transit/value ret) value)))

  ;; Msgpack
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :msgpack))
        ctx   {:serval.service/request {:input-stream in}}
        ret   (srv.transit/read-transit ctx {:type :msgpack})]
    (is (= (:serval.transit/value ret) value)))

  ;; To/from
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (transit-bytes value :json))
        ctx   {:input in}
        ret   (srv.transit/read-transit ctx {:from [:input]
                                             :to   [:output]})]
    (is (= (:output ret) value))))

(deftest test-read-transit-error
  (let [in    (ByteArrayInputStream. (.getBytes "foo"))
        ctx   {:serval.service/request {:input-stream in}}
        ret   (srv.transit/read-transit ctx)]
    (is (:serval.transit/error ret))))

(deftest test-transit-body
  (let [value {:foo "bar"}]
    (with-response {:serval.response/body (srv.transit/transit-body value)}
      (let [resp       (send nil :input-stream)
            reader     (transit/reader (:body resp) :json)
            read-value (transit/read reader)]
        (is (= read-value value))))))
