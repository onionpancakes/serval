(ns dev.onionpancakes.serval.core.tests.test-core
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is are]]
            [clojure.string]))

(deftest test-set-http
  (with-handler (fn [_ _ response]
                  (srv/set-http response
                                :status             400
                                :headers            {:foo "bar"}
                                :locale             (java.util.Locale/ENGLISH)
                                :content-type       "text/html"
                                :character-encoding "utf-8"))
    (let [resp (send)]
      (is (= (:status resp) 400))
      (is (= (get-in (:headers resp) ["foo" 0]) "bar"))
      (is (= (get-in (:headers resp) ["content-language" 0]) "en"))
      (is (= (:media-type resp) "text/html"))
      (is (= (:character-encoding resp) "utf-8"))))
  ;; Empty case
  (with-handler (fn [_ _ response]
                  (srv/set-http response))
    (let [resp (send)]
      (is (= (:status resp) 200)))))

(deftest test-write-body
  (are [body expected] (with-handler (fn [_ _ response]
                                       (apply srv/write-body response body))
                         (= (:body (send)) expected))
    []            ""
    ["foo"]       "foo"
    ["foo" "bar"] "foobar"))

(deftest test-send-error
  (with-handler (fn [_ _ response]
                  (srv/send-error response 400))
    (is (= (:status (send)) 400)))
  (with-handler (fn [_ _ response]
                  (srv/send-error response 400 "FOOBAR"))
    (let [resp (send)]
      (is (= (:status resp) 400))
      (is (clojure.string/includes? (:body resp) "FOOBAR")))))

(deftest test-send-redirect
  (with-handler (fn [_ _ response]
                  (srv/send-redirect response "/"))
    (is (= (:status (send)) 302))))
