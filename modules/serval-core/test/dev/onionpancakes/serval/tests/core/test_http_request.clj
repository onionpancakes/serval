(ns dev.onionpancakes.serval.tests.core.test-http-lookup
  (:require [dev.onionpancakes.serval.io.http :as http]
            [dev.onionpancakes.serval.mock.http :as mock]
            [clojure.test :refer [deftest are]])
  (:import [java.util Locale]))

(def mock-data
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
   :locales            [(Locale. "en")]})

(deftest test-request-lookup
  (let [req (-> (mock/http-servlet-request mock-data "")
                (http/servlet-request-proxy))]
    (are [path expected] (= (get-in req path) expected)
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
      [:character-encoding]       "UTF-8")))

(deftest test-request-not-found
  (let [req (-> (mock/http-servlet-request {} "")
                (http/servlet-request-proxy))]
    (are [path nf expected] (= (get-in req path nf) expected)
      [:foo]              "bar" "bar"
      [:attributes "foo"] "bar" "bar"
      [:headers "foo"]    "bar" "bar"
      [:attribute-names]  "bar" "bar"
      [:header-names]     "bar" "bar"
      [:locales]          "bar" "bar"
      [:cookies]          "bar" "bar")))
