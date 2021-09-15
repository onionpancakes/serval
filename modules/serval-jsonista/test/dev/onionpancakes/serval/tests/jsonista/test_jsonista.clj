(ns dev.onionpancakes.serval.tests.jsonista.test-jsonista
  (:require [dev.onionpancakes.serval.io.body :as io.body]
            [dev.onionpancakes.serval.jsonista :as sj]
            [dev.onionpancakes.serval.mock.http :as mock]
            [jsonista.core :as j]
            [clojure.test :refer [deftest is]])
  (:import [java.io StringReader]))

(deftest test-read-json
  (let [rdr (StringReader. "{\"foo\": \"bar\"}")
        ctx {:serval.service/request {:reader rdr}}
        ret (sj/read-json ctx)]
    (is (= {"foo" "bar"} (:serval.request/body ret))))

  ;; Error
  (let [rdr (StringReader. "{\"foo\": }")
        ctx {:serval.service/request {:reader rdr}}
        ret (sj/read-json ctx)]
    (is (:serval.jsonista/error ret)))

  ;; Opts
  (let [rdr  (StringReader. "{\"foo\": \"bar\"}")
        ctx  {:json-from rdr}
        opts {:from          [:json-from]
              :to            [:json-to]
              :object-mapper j/keyword-keys-object-mapper}
        ret  (sj/read-json ctx opts)]
    (is (= {:foo "bar"} (:json-to ret)))))

(deftest test-json-body
  ;; Sync
  (let [req   (mock/http-servlet-request {} "")
        resp  (mock/http-servlet-response {} req)
        ctx   {:serval.service/request  req
               :serval.service/response resp}
        jbody (sj/json-body {:foo "bar"})
        _     (io.body/write-body jbody ctx)]
    (is (= "{\"foo\":\"bar\"}" (slurp (.toByteArray (:output-stream resp))))))

  ;; Async
  (let [req   (mock/http-servlet-request {} "")
        resp  (mock/http-servlet-response {} req)
        ctx   {:serval.service/request  req
               :serval.service/response resp}
        jbody (io.body/async-body (sj/json-body {:foo "bar"}))
        _     (.startAsync req)
        cf    (io.body/write-body jbody ctx)
        _     (.get cf)]
    (is (= "{\"foo\":\"bar\"}" (slurp (.toByteArray (:output-stream resp)))))))
