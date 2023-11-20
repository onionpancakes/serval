(ns dev.onionpancakes.serval.core.tests.test-servlet
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.servlet :as srv.servlet]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is]]))

(deftest test-servlet
  (with-handler (srv.servlet/servlet srv/response 200 "foo")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "foo")))))

(deftest test-filter
  (with-handler [["/*"
                  (srv.servlet/filter srv/send-error 400)
                  (srv.servlet/servlet srv/response 200 "foo")]]
    (let [ret (send)]
      (is (= (:status ret) 400)))))
