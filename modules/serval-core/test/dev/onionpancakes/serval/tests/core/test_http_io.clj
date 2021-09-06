(ns dev.onionpancakes.serval.tests.core.test-http-io
  (:require [dev.onionpancakes.serval.io.http :as h]
            [dev.onionpancakes.serval.mock :as mock]
            [clojure.test :refer [deftest is are]]))

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

(deftest test-write-response
  (is false))

(deftest test-service-fn
  (is false))
