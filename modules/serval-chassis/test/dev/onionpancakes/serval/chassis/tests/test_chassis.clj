(ns dev.onionpancakes.serval.chassis.tests.test-chassis
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [dev.onionpancakes.serval.chassis :as srv.html]
            [clojure.test :refer [deftest is]]))

(deftest test-json-writable
  (with-handler (fn [_ _ response]
                  (let [body (-> [:div {:id "foo"} "bar"]
                                 (srv.html/html-writable))]
                    (srv/write-body response body)))
    (let [resp (send)]
      (is (= (:body resp) "<div id=\"foo\">bar</div>")))))
