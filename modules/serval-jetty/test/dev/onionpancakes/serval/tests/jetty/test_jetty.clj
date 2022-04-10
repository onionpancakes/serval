(ns dev.onionpancakes.serval.tests.jetty.test-jetty
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.server :as server]
            [dev.onionpancakes.serval.test-utils.client
             :refer [send]]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [clojure.test :refer [is deftest]])
  (:import [java.util.zip GZIPInputStream]))

;; Server config macro

(defmacro with-config
  [config & body]
  `(try
     (.stop server/server)
     (srv.jetty/configure-server! server/server ~config)
     (.start server/server)
     ~@body
     (finally
       (.stop server/server)
       (.join server/server)
       (srv.jetty/configure-server! server/server {:connectors []
                                                   :handler    nil}))))

;; Tests

(deftest test-minimal
  (with-config {:connectors [{:port 42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (str (:version resp)) "HTTP_1_1"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-http1
  (with-config {:connectors [{:protocol :http
                              :port     42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (str (:version resp)) "HTTP_1_1"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-http2c
  (with-config {:connectors [{:protocol :http2c
                              :port     42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (str (:version resp)) "HTTP_2"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-multiple-connectors
  (with-config {:connectors [{:protocol :http
                              :port     42000}
                             {:protocol :http2c
                              :port     42001}]
                :handler    #(case (int (:server-port (:serval.service/request %)))
                               42000 {:serval.response/body "foo"}
                               42001 {:serval.response/body "bar"})}
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (str (:version resp)) "HTTP_1_1"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send {:uri "http://localhost:42001"})]
      (is (= (str (:version resp)) "HTTP_2"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))))

(deftest test-gzip-handler
  (with-config {:connectors [{:protocol :http
                              :port     42000}]
                :handler    (-> (constantly {:serval.response/body "foo"})
                                (srv.jetty/gzip-handler {:min-gzip-size 0}))}
    (let [req  {:uri     "http://localhost:42000"
                :headers {"accept-encoding" ["gzip"]}} 
          resp (send req :input-stream)]
      (is (= (:status resp) 200))
      (is (= (slurp (GZIPInputStream. (:body resp))) "foo")))))

(deftest test-servlet-context-spec
  (with-config {:connectors [{:protocol :http
                              :port     42000}]
                :handler    [["/foo" (constantly {:serval.response/body "foo"})]
                             ["/bar" (constantly {:serval.response/body "bar"})]
                             ["/*"   (constantly {:serval.response/body "default"})]]}
    (let [resp (send {:uri "http://localhost:42000/foo"})]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send {:uri "http://localhost:42000/bar"})]
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (:status resp) 200))
      (is (= (:body resp) "default")))))

(defn var-handler
  [ctx]
  {:serval.response/body "foo"})

(deftest test-var-handler
  (with-config {:connectors [{:protocol :http
                              :port     42000}]
                :handler    #'var-handler}
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))
