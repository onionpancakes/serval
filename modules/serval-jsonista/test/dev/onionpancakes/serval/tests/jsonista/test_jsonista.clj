(ns dev.onionpancakes.serval.tests.jsonista.test-jsonista
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [dev.onionpancakes.serval.test-utils.server
             :refer [with-response]]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [clojure.test :refer [deftest is]])
  (:import [java.io StringReader]))

(deftest test-read-json
  (let [rdr (StringReader. "{\"foo\": \"bar\"}")
        ctx {:serval.service/request {:reader rdr}}
        ret (srv.json/read-json ctx)]
    (is (= {"foo" "bar"} (:serval.jsonista/value ret))))

  ;; Error
  (let [rdr (StringReader. "{\"foo\": }")
        ctx {:serval.service/request {:reader rdr}}
        ret (srv.json/read-json ctx)]
    (is (:serval.jsonista/error ret)))

  ;; Empty
  (let [rdr (StringReader. "")
        ctx {:serval.service/request {:reader rdr}}
        ret (srv.json/read-json ctx)]
    (is (:serval.jsonista/error ret)))

  ;; Opts
  (let [rdr  (StringReader. "{\"foo\": \"bar\"}")
        ctx  {:json-from rdr}
        opts {:from          [:json-from]
              :to            [:json-to]
              :object-mapper srv.json/keyword-keys-object-mapper}
        ret  (srv.json/read-json ctx opts)]
    (is (= {:foo "bar"} (:json-to ret)))))

(deftest test-json-body
  (with-response {:serval.response/body (srv.json/json-body {:foo "bar"})}
    (is (= (:body (send)) "{\"foo\":\"bar\"}"))))
