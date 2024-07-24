(ns dev.onionpancakes.serval.core.tests.test-core
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty-test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is are]]
            [clojure.string])
  (:import [jakarta.servlet.http HttpServletResponse]))

;; Request

(deftest test-get-input-stream
  (with-handler (fn [_ request response]
                  (srv/set-http response {:content-type       "text/plain"
                                          :character-encoding "utf-8"})
                  (->> (srv/get-input-stream request)
                       (slurp)
                       (srv/write-body response)))
    (let [resp (send {:method :POST
                      :body   "foo"})]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-get-reader
  (with-handler (fn [_ request response]
                  (srv/set-http response {:content-type       "text/plain"
                                          :character-encoding "utf-8"})
                  (->> (srv/get-reader request)
                       (slurp)
                       (srv/write-body response)))
    (let [resp (send {:method :POST
                      :body   "foo"})]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-get-set-attribute
  (with-handler (fn [_ request response]
                  (srv/set-attribute request "foo" "bar")
                  (srv/write-body response (srv/get-attribute request "foo")))
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))))

;; Response

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
      (is (= (:status resp) 200))))
  ;; Return value
  (with-handler (fn [_ _ response]
                  (if-not (instance? HttpServletResponse (srv/set-http response))
                    (throw (ex-info "Not HttpServletResponse" {}))))
    (let [resp (send)]
      (is (= (:status resp) 200)))))

(deftest test-write-body
  (are [body expected] (with-handler (fn [_ _ response]
                                       (apply srv/write-body response body))
                         (= (:body (send)) expected))
    []            ""
    ["foo"]       "foo"
    ["foo" "bar"] "foobar")
  ;; Return value
  (with-handler (fn [_ _ response]
                  (if-not (instance? HttpServletResponse (srv/write-body response))
                    (throw (ex-info "Not HttpServletResponse" {}))))
    (let [resp (send)]
      (is (= (:status resp) 200)))))

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

(deftest test-get-output-stream
  (with-handler (fn [_ request response]
                  (srv/set-http response {:content-type       "text/plain"
                                          :character-encoding "utf-8"})
                  (-> (srv/get-output-stream response)
                      (spit "foo")))
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-get-writer
  (with-handler (fn [_ request response]
                  (srv/set-http response {:content-type       "text/plain"
                                          :character-encoding "utf-8"})
                  (-> (srv/get-writer response)
                      (spit "foo")))
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-writable-to-output-stream
  (with-handler (fn [_ request response]
                  (let [body (srv/writable-to-output-stream (.getBytes "foo" "utf-8"))]
                    (doto response
                      (srv/set-http {:content-type       "text/plain"
                                     :character-encoding "utf-8"})
                      (srv/write-body body))))
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-writable-to-writer
  (with-handler (fn [_ request response]
                  (let [body (srv/writable-to-writer "foo")]
                    (doto response
                      (srv/set-http {:content-type       "text/plain"
                                     :character-encoding "utf-8"})
                      (srv/write-body body))))
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

;; Filter

(deftest test-do-filter
  (with-handler {:routes [["/*" (fn [_ req resp chain]
                                  (srv/do-filter chain req resp)) (constantly nil)]]}
    (is (= (:status (send)) 200))))
