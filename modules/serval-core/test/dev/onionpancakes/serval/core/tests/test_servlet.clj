(ns dev.onionpancakes.serval.core.tests.test-servlet
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.servlet :as srv.servlet]
            [dev.onionpancakes.serval.jetty-test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is]]))

(defn example-service-fn
  [_ req resp]
  (srv/write-body resp "foo"))

(deftest test-servlet
  (with-handler (srv.servlet/servlet example-service-fn)
    (let [resp (send)]
      (is (= (:body resp) "foo")))))

(defn example-do-filter-fn
  [_ _ resp _]
  (srv/write-body resp "foo"))

(deftest test-filter
  (with-handler {:routes [["/*" (srv.servlet/filter example-do-filter-fn) example-service-fn]]}
    (let [resp (send)]
      (is (= (:body resp) "foo")))))

(defn example-pred
  [request]
  (case (:method request)
    :GET  true
    :POST false
    false))

(deftest test-pred-filter
  (with-handler {:routes [["/*" (srv.servlet/pred-filter example-pred 400) example-service-fn]]}
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send {:method :POST})]
      (is (= (:status resp) 400)))))

(deftest test-http-method-filter
  (with-handler {:routes [["/*" (srv.servlet/http-method-filter #{:GET}) example-service-fn]]}
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send {:method :POST})]
      (is (= (:status resp) 405)))))
