(ns dev.onionpancakes.serval.tests.mock.test-http-request
  (:require [dev.onionpancakes.serval.mock.http :as http]
            [dev.onionpancakes.serval.mock.io :as io]
            [clojure.test :refer [deftest is]])
  (:import [jakarta.servlet DispatcherType]
           [jakarta.servlet.http HttpServletRequest Cookie]
           [java.util Locale]
           [java.io ByteArrayOutputStream]))

(def request-data
  {:attributes         {"Foo" "Bar"}
   :remote-addr        "some remote addr"
   :remote-host        "some remote host"
   :remote-port        3000
   :local-addr         "some local addr"
   :local-name         "some local name"
   :local-port         3001
   :dispatcher-type    DispatcherType/REQUEST
   :scheme             "http"
   :server-name        "some server name"
   :server-port        3002
   :path               "/somepath"
   :context-path       "/somecontextpath"
   :servlet-path       "/someservletpath"
   :path-info          "/somepathinfo"
   :query-string       "?somequerystring=value"
   :parameter-map      {"somequerystring" ["value"]}
   :protocol           "HTTP/1.1"
   :method             "GET"
   :headers            {"Foo" ["Bar"]}
   :content-length     8
   :content-type       "text/plain"
   :character-encoding "utf-8"
   :locales            [(Locale. "en")]
   :cookies            [(Cookie. "Foo" "Bar")]})

(deftest test-get-methods
  (let [req (http/http-servlet-request request-data "")]
    (is (= (.getAttribute req "Foo") "Bar"))
    (is (= (.getRemoteAddr req) "some remote addr"))
    (is (= (.getRemoteHost req) "some remote host"))
    (is (= (.getRemotePort req) 3000))
    (is (= (.getLocalAddr req) "some local addr"))
    (is (= (.getLocalName req) "some local name"))
    (is (= (.getLocalPort req) 3001))
    (is (= (.getDispatcherType req) DispatcherType/REQUEST))
    (is (= (.getScheme request) "http"))
    (is (= (.getServerName req) "some server name"))
    (is (= (.getServerPort req) 3002))
    (is (= (.getRequestURI req) "/somepath"))
    (is (= (.getContextPath req) "/somecontextpath"))
    (is (= (.getServletPath req) "/someservletpath"))
    (is (= (.getPathInfo req) "/somepathinfo"))
    (is (= (.getQueryString req) "?somequerystring=value"))
    (is (= (into {} (map (juxt key (comp vec val)))
                 (.getParameterMap req)) {"somequerystring" ["value"]}))
    (is (= (.getProtocol req) "HTTP/1.1"))
    (is (= (.getMethod req) "GET"))
    (is (= (enumeration-seq (.getHeaders req "Foo")) ["Bar"]))
    (is (= (enumeration-seq (.getHeaderNames req)) ["Foo"]))
    (is (= (.getContentLength req) 8))
    (is (= (.getContentLengthLong req) 8))
    (is (= (.getContentType req) "text/plain"))
    (is (= (.getCharacterEncoding req) "utf-8"))
    ;; getLocale not supported
    (is (= (enumeration-seq (.getLocales req)) [(Locale. "en")]))
    ;; Cookies can't be compared by value.
    ;; Need to compare to by identity.
    (is (= (vec (.getCookies req)) (:cookies request-data)))))

(deftest test-input-stream
  (let [req (http/http-servlet-request {} "Foobar")
        in  (.getInputStream req)]
    (is (= (slurp in :encoding "utf-8") "Foobar"))
    (is (thrown? IllegalStateException (.getReader req)))))

(deftest test-reader
  (let [req (http/http-servlet-request {} "Foobar")
        rdr (.getReader req)]
    (is (= (slurp rdr :encoding "utf-8") "Foobar"))
    (is (thrown? IllegalStateException (.getInputStream req)))))

(deftest test-async-context
  (let [req (http/http-servlet-request {} "")]
    (is (thrown? IllegalStateException (.getAsyncContext req)))
    (is (.isAsyncSupported req))
    (let [a (.startAsync req)]
      (is (.isAsyncStarted req))
      (.complete a)
      (is (:async-complete? (deref (:data a))))
      (is (not (.isAsyncStarted req))))))

(deftest test-read-async
  (let [req (http/http-servlet-request {} "Foobar")
        in  (.getInputStream req)
        out (ByteArrayOutputStream.)]
    (is (thrown? NullPointerException (.setReadListener in nil)))
    (is (thrown? IllegalStateException (io/read-async! in out)))
    (.startAsync req)
    (io/read-async! in out)
    (is (= (slurp (.toByteArray out) :encoding "utf-8") "Foobar"))))

(defn run-tests
  []
  (clojure.test/run-tests 'dev.onionpancakes.serval.tests.mock.test-http-request))
