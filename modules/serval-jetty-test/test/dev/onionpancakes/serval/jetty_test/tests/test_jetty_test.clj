(ns dev.onionpancakes.serval.jetty-test.tests.test-jetty-test
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty-test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is]])
  (:import [jakarta.servlet.http HttpServletResponse]))

(defn example-handler
  [_ _ ^HttpServletResponse response]
  (.setStatus response 200)
  (.write (.getWriter response) "foo"))

(deftest test-with-handler
  (with-handler example-handler
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))
