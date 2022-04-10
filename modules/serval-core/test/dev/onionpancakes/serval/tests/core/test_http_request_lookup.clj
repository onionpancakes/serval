(ns dev.onionpancakes.serval.tests.core.test-http-request-lookup
  (:require [dev.onionpancakes.serval.io.http
             :refer [servlet-request-lookup-proxy]]
            [clojure.test :refer [deftest is are]])
  (:import [java.util Collections]
           [jakarta.servlet.http HttpServletRequest]))

(def mock-http-servlet-request
  (reify HttpServletRequest
    (getAttribute [_ attr]
      (case attr
        "foo"   :foo
        "false" false
        nil))
    (getAttributeNames [_]
      (Collections/enumeration ["foo" "false"]))))

(def lookup-proxy
  (servlet-request-lookup-proxy mock-http-servlet-request))

(deftest test-request-lookup
  (are [path expected] (= (get-in lookup-proxy path) expected)
    [:attributes "foo"]       :foo
    [:attributes "false"]     false
    [:attributes "not-found"] nil

    [:attribute-names] ["foo" "false"]))
