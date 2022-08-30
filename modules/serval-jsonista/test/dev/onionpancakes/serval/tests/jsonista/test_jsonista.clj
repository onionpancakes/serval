(ns dev.onionpancakes.serval.tests.jsonista.test-jsonista
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [dev.onionpancakes.serval.test-utils.server
             :refer [with-response]]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as j])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn json-bytes
  [value]
  (let [out (ByteArrayOutputStream.)
        _   (j/write-value out value)]
    (.toByteArray out)))

(deftest test-read-json
  (let [value {"foo" "bar"}
        in    (ByteArrayInputStream. (json-bytes value))
        ctx   {:serval.service/request {:input-stream in}}
        ret   (srv.json/read-json ctx)]
    (is (= (:serval.jsonista/value ret) value)))

  ;; Keyword keys
  (let [value {:foo "bar"}
        in    (ByteArrayInputStream. (json-bytes value))
        ctx   {:serval.service/request {:input-stream in}}
        ret   (srv.json/read-json ctx srv.json/keyword-keys-object-mapper)]
    (is (= (:serval.jsonista/value ret) value))))

(deftest test-read-json-error
  (let [in  (ByteArrayInputStream. (.getBytes "{foo"))
        ctx {:serval.service/request {:input-stream in}}
        ret (srv.json/read-json ctx)]
    (is (:serval.jsonista/error ret))))

(deftest test-json-body
  (let [value {"foo" "bar"}]
    (with-response {:serval.response/body (srv.json/json-body value)}
      (let [resp       (send)
            resp-value (j/read-value (:body resp))]
        (is (= resp-value value))))))
