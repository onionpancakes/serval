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
   :cookies            [(Cookie. "Foo" "Bar")]
   })

(def ^HttpServletRequest request
  (http/http-servlet-request request-data ""))

(deftest test-request-data-methods
  (is (= (.getAttribute request "Foo") "Bar"))
  (is (= (.getRemoteAddr request) "some remote addr"))
  (is (= (.getRemoteHost request) "some remote host"))
  (is (= (.getRemotePort request) 3000))
  (is (= (.getLocalAddr request) "some local addr"))
  (is (= (.getLocalName request) "some local name"))
  (is (= (.getLocalPort request) 3001))
  (is (= (.getDispatcherType request) DispatcherType/REQUEST))
  (is (= (.getScheme request) "http"))
  (is (= (.getServerName request) "some server name"))
  (is (= (.getServerPort request) 3002))
  (is (= (.getRequestURI request) "/somepath"))
  (is (= (.getContextPath request) "/somecontextpath"))
  (is (= (.getServletPath request) "/someservletpath"))
  (is (= (.getPathInfo request) "/somepathinfo"))
  (is (= (.getQueryString request) "?somequerystring=value"))
  (is (= (into {} (map (juxt key (comp vec val)))
               (.getParameterMap request)) {"somequerystring" ["value"]}))
  (is (= (.getProtocol request) "HTTP/1.1"))
  (is (= (.getMethod request) "GET"))
  (is (= (enumeration-seq (.getHeaders request "Foo")) ["Bar"]))
  (is (= (enumeration-seq (.getHeaderNames request)) ["Foo"]))
  (is (= (.getContentLength request) 8))
  (is (= (.getContentLengthLong request) 8))
  (is (= (.getContentType request) "text/plain"))
  (is (= (.getCharacterEncoding request) "utf-8"))
  ;; getLocale not supported
  (is (= (enumeration-seq (.getLocales request)) [(Locale. "en")]))
  ;; Cookies can't be compared by value.
  ;; Need to compare to by identity.
  (is (= (vec (.getCookies request)) (:cookies request-data))))

(def ^HttpServletRequest request-read-in
  (http/http-servlet-request {} "Foobar"))

(deftest test-request-read-input-stream
  (let [in  (.getInputStream request-read-in)]
    (is (= (slurp in :encoding "utf-8") "Foobar"))
    (is (thrown? IllegalStateException (.getReader request-read-in)))))

(def ^HttpServletRequest request-read-reader
  (http/http-servlet-request {} "Foobar"))

(deftest test-request-read-reader
  (let [rdr (.getReader request-read-reader)]
    (is (= (slurp rdr :encoding "utf-8") "Foobar"))
    (is (thrown? IllegalStateException (.getInputStream request-read-reader)))))

(def ^HttpServletRequest request-async
  (http/http-servlet-request {} ""))

(deftest test-request-async-context
  (is (thrown? IllegalStateException (.getAsyncContext request-async)))
  (is (.isAsyncSupported request-async))
  (let [a (.startAsync request-async)]
    (is (.isAsyncStarted request-async))
    (.complete a)
    (is (:completed? (deref (:data a))))
    (is (not (.isAsyncStarted request-async)))))

(def ^HttpServletRequest request-read-async
  (http/http-servlet-request {} "Foobar"))

(deftest test-request-read-async
  (let [in  (.getInputStream request-read-async)
        out (ByteArrayOutputStream.)]
    (is (thrown? NullPointerException (.setReadListener in nil)))
    (is (thrown? IllegalStateException (io/read-async! in out)))
    (.startAsync request-read-async)
    (io/read-async! in out)
    (is (= (slurp (.toByteArray out) :encoding "utf-8") "Foobar"))))
