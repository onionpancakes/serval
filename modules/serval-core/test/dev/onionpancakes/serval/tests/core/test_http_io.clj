(ns dev.onionpancakes.serval.tests.core.test-http-io
  (:require [dev.onionpancakes.serval.io.http :as h]
            [dev.onionpancakes.serval.io.body :as b]
            [dev.onionpancakes.serval.mock :as mock]
            [clojure.test :refer [deftest is are]])
  (:import [java.util.concurrent CompletionStage CompletableFuture]))

(def servlet-request-data
  {:attributes         {"attr1" "foobar-attr"}
   :remote-addr        "some remote addr"
   :remote-host        "some remote host"
   :remote-port        3000
   :local-addr         "some local addr"
   :local-name         "some local name"
   :local-port         3001
   :scheme             "http"
   :server-name        "some server name"
   :server-port        3002
   :path               "/somepath"
   :context-path       "/somecontextpath"
   :servlet-path       "/someservletpath"
   :path-info          "/somepathinfo"
   :query-string       "?somequerystring=foo"
   :parameter-map      {"somequerystring" ["foo"]}
   :protocol           "HTTP/1.1"
   :method             "GET"
   :headers            {"Content-Type" ["text/plain"]}
   :content-length     0
   :content-type       "application/javascript"
   :character-encoding "UTF-8"
   ;; Test locales?

   })

(def request
  (-> servlet-request-data
      (mock/mock-http-servlet-request-string "Foobar" "utf-8")
      (h/servlet-request-proxy)))

(deftest test-servlet-request-proxy-lookup
  (are [path expected] (= (get-in request path) expected)
    [:attributes "attr1"]       "foobar-attr"
    [:attribute-names]          ["attr1"]
    [:remote-addr]              "some remote addr"
    [:remote-host]              "some remote host"
    [:remote-port]              3000
    [:local-addr]               "some local addr"
    [:local-name]               "some local name"
    [:local-port]               3001
    [:scheme]                   "http"
    [:server-name]              "some server name"
    [:server-port]              3002
    [:path]                     "/somepath"
    [:context-path]             "/somecontextpath"
    [:servlet-path]             "/someservletpath"
    [:path-info]                "/somepathinfo"
    [:query-string]             "?somequerystring=foo"
    [:parameter-map]            {"somequerystring" ["foo"]}
    [:protocol]                 "HTTP/1.1"
    [:method]                   :GET
    [:headers "Content-Type" 0] "text/plain"
    [:content-length]           0
    [:content-type]             "application/javascript"
    [:character-encoding]       "UTF-8"))

(def example-response
  {:serval.response/status  200
   :serval.response/headers {"Foo" [0 (int 1) "Bar"]}
   :serval.response/body    "Foobar"})

(deftest test-example-response
  (let [req  (mock/mock-http-servlet-request-string {} "Foobar" "utf-8")
        resp (mock/mock-http-servlet-response {} req)
        ctx  {:serval.service/request  req
              :serval.service/response resp}
        res  (h/write-response example-response ctx)]
    (is (not (h/async-response? example-response ctx)))
    (is (nil? res))
    (is (= (:status @(:data resp)) 200))
    (is (= (:headers @(:data resp)) {"Foo" [0 1 "Bar"]}))
    (is (= (str (:writer resp)) "Foobar"))))

(def example-response-async-body
  {:serval.response/body (b/async-body "Foobar")})

(deftest test-example-response-async-body
  (let [req  (mock/mock-http-servlet-request-string {} "Foobar" "utf-8")
        resp (mock/mock-http-servlet-response {} req)
        ctx  {:serval.service/request  req
              :serval.service/response resp}
        res  (h/write-response example-response-async-body ctx)]
    (is (h/async-response? example-response-async-body ctx))
    (is (instance? CompletionStage res))
    (.get (.toCompletableFuture res)) ; Wait till resolved
    (is (= (String. (.toByteArray (:output-stream resp))) "Foobar"))))

(def example-response-cs
  (-> {:serval.response/status 200}
      (CompletableFuture/completedStage)))

(deftest test-example-response-cs
  (let [req  (mock/mock-http-servlet-request-string {} "Foobar" "utf-8")
        resp (mock/mock-http-servlet-response {} req)
        ctx  {:serval.service/request  req
              :serval.service/response resp}
        res  (h/write-response example-response-cs ctx)]
    (is (h/async-response? example-response-async-body ctx))
    (is (instance? CompletionStage res))
    (.get (.toCompletableFuture res))
    (is (= (:status @(:data resp)) 200))))

(deftest test-service-fn
  (is false))
