(ns dev.onionpancakes.serval.jsonista.tests.test-jsonista
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty.test
             :refer [with-response send]]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json])
  (:import [jakarta.servlet ServletRequest]
           [java.io BufferedReader CharArrayReader CharArrayWriter]))

(defn json-reader
  [value]
  (with-open [wtr (doto (CharArrayWriter.)
                    (json/write-value value))]
    (-> (CharArrayReader. (.toCharArray wtr))
        (BufferedReader.))))

(defn context-with-json
  [value]
  {:serval.context/request (reify ServletRequest
                             (getReader [this]
                               (json-reader value)))})

(deftest test-read-json
  (let [value {"foo" "bar"}
        ctx   (context-with-json value)
        ret   (srv.json/read-json ctx)]
    (is (= (:serval.jsonista/value ret) value)))

  ;; Custom value key
  (let [value {"foo" "bar"}
        ctx   (context-with-json value)
        ret   (srv.json/read-json ctx {:value-key :value})]
    (is (= (:value ret) value)))

  ;; Keyword keys object mapper
  (let [value {:foo "bar"}
        ctx   (context-with-json value)
        ret   (srv.json/read-json ctx {:object-mapper srv.json/keyword-keys-object-mapper})]
    (is (= (:serval.jsonista/value ret) value))))

(defn string-reader
  [^String value]
  (-> (CharArrayReader. (.toCharArray value))
      (BufferedReader.)))

(defn context-with-string
  [value]
  {:serval.context/request (reify ServletRequest
                             (getReader [this]
                               (string-reader value)))})

(deftest test-read-json-error
  (let [ctx (context-with-string "{foo")
        ret (srv.json/read-json ctx)]
    (is (:serval.jsonista/error ret)))

  ;; Custom error key
  (let [ctx (context-with-string "{foo")
        ret (srv.json/read-json ctx {:error-key :error})]
    (is (:error ret))))

(deftest test-json-body
  (let [value {"foo" "bar"}]
    (with-response {:serval.response/body (srv.json/json-value value)}
      (let [resp       (send)
            resp-value (json/read-value (:body resp))]
        (is (= resp-value value))))))
