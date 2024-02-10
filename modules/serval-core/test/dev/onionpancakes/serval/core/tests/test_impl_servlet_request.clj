(ns dev.onionpancakes.serval.core.tests.test-impl-servlet-request
  (:require [dev.onionpancakes.serval.impl.http.servlet-request
             :refer [servlet-request-proxy]]
            [clojure.test :refer [deftest is are]])
  (:import [java.io StringReader BufferedReader]
           [java.util Collections HashMap Locale]
           [jakarta.servlet DispatcherType ServletInputStream]
           [jakarta.servlet.http HttpServletRequest Cookie]))

(def mock-http-servlet-request
  (reify HttpServletRequest
    (getAttribute [_ k] (if (= k "foo") "foo"))
    (getAttributeNames [_]
      (Collections/enumeration ["foo"]))
    (getRemoteAddr [_] "foo")
    (getRemoteHost [_] "foo")
    (getRemotePort [_] 3000)
    (getLocalAddr [_] "foo")
    (getLocalName [_] "foo")
    (getLocalPort [_] 3000)
    (getDispatcherType [_] DispatcherType/REQUEST)
    (getScheme [_] "foo")
    (getServerName [_] "foo")
    (getServerPort [_] 3000)
    (getRequestURI [_] "foo")
    (getContextPath [_] "foo")
    (getServletPath [_] "foo")
    (getPathInfo [_] "foo")
    (getQueryString [_] "foo")
    (getParameterMap [_] (doto (HashMap.)
                           (.put "foo" (into-array String ["bar"]))))
    (getProtocol [_] "foo")
    (getMethod [_] "foo")
    (getHeader [_ k] (if (= k "foo") "foo"))
    (getHeaders [_ _] (Collections/enumeration ["foo"]))
    (getHeaderNames [_] (Collections/enumeration ["foo"]))
    (getContentLengthLong [_] 0)
    (getContentType [_] "foo")
    (getCharacterEncoding [_] "foo")
    (getLocales [_] (Collections/enumeration [(Locale. "en")]))
    (getCookies [_] (->> []
                         (into-array Cookie)))
    (getReader [_] (BufferedReader. (StringReader. "foo")))
    (getInputStream [_] (proxy [ServletInputStream] []))))

(def example-servlet-request-proxy
  (servlet-request-proxy mock-http-servlet-request))

(deftest test-servlet-request-proxy-lookup
  (are [path expected] (= (get-in example-servlet-request-proxy path) expected)
    [:attributes "foo"]   "foo"
    [:remote-addr]        "foo"
    [:remote-host]        "foo"
    [:remote-port]        3000
    [:local-addr]         "foo"
    [:local-name]         "foo"
    [:local-port]         3000
    [:dispatcher-type]    DispatcherType/REQUEST
    [:scheme]             "foo"
    [:server-name]        "foo"
    [:server-port]        3000
    [:path]               "foo"
    [:context-path]       "foo"
    [:servlet-path]       "foo"
    [:path-info]          "foo"
    [:query-string]       "foo"
    [:parameters "foo" 0] "bar"
    [:protocol]           "foo"
    [:method]             :foo
    [:headers "foo" 0]    "foo"
    [:content-length]     0
    [:content-type]       "foo"
    [:character-encoding] "foo"
    [:locales]            [(Locale. "en")]))

(deftest test-servlet-request-proxy-lookup-not-found
  (is (= (get example-servlet-request-proxy ::foo :not-found) :not-found)))

(deftest test-attributes-proxy-lookup-not-found
  (is (= (get-in example-servlet-request-proxy [:attributes "NO_FOO"] :not-found) :not-found)))

(deftest test-headers-proxy-lookup-not-found
  (is (= (get-in example-servlet-request-proxy [:headers "NO_FOO"] :not-found) :not-found)))
