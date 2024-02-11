(ns dev.onionpancakes.serval.chassis.tests.test-chassis
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty.test
             :refer [with-response send]]
            [dev.onionpancakes.serval.chassis :as srv.html]
            [clojure.test :refer [deftest is]]))

(deftest test-html-writable
  (let [value [:div {:id "foo"} "bar"]
        body  (srv.html/html-writable value)]
    (with-response {:serval.response/body body}
      (let [result (:body (send))]
        (is (= result "<div id=\"foo\">bar</div>"))))))
