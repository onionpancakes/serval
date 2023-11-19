(ns dev.onionpancakes.serval.jetty.tests.test-jetty
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core
             :refer [response]]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-config send]]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [clojure.test :refer [is deftest]])
  (:import [jakarta.servlet.http HttpServletResponse]
           [java.util.zip GZIPInputStream]
           [org.eclipse.jetty.server Handler$Abstract Request Response]
           [org.eclipse.jetty.util Callback]))

(deftest test-minimal
  (with-config {:connectors [{:port 42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (str (:version resp)) "HTTP_1_1"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-http1
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (str (:version resp)) "HTTP_1_1"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-http2c
  (with-config {:connectors [{:protocol :http2c :port 42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (str (:version resp)) "HTTP_2"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-multiple-connectors
  (with-config {:connectors [{:protocol :http :port 42000}
                             {:protocol :http2c :port 42001}]
                :handler    #(case (long (:server-port (:serval.context/request %)))
                               42000 {:serval.response/body "foo"}
                               42001 {:serval.response/body "bar"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (str (:version resp)) "HTTP_1_1"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42001")]
      (is (= (str (:version resp)) "HTTP_2"))
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))))

(defn example-var-handler
  [ctx]
  {:serval.response/body "foo"})

(deftest test-var-handler
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    #'example-var-handler}
    (let [resp (send "http://localhost:42000")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(def example-routes
  [["/foo" (constantly {:serval.response/body "foo"})]
   ["/bar" (constantly {:serval.response/body "bar"})]
   ["/*"   (constantly {:serval.response/body "default"})]])

(deftest test-routes-handler
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    example-routes}
    (let [resp (send "http://localhost:42000/foo")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42000/bar")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))
    (let [resp (send "http://localhost:42000")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "default"))))
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    {:routes example-routes}}
    (let [resp (send "http://localhost:42000/foo")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42000/bar")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))
    (let [resp (send "http://localhost:42000")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "default")))))

(defn example-route-send-error-code-handler
  [{:serval.context/keys [^HttpServletResponse response] :as ctx}]
  (.sendError response 500)
  ctx)

(defn example-route-throw-handler
  [ctx]
  (throw (ex-info "foobar" {}))
  ctx)

(defn example-route-error-code-handler
  [ctx]
  (response ctx 500 "handled-error-code"))

(defn example-route-throwable-handler
  [ctx]
  (response ctx 500 "handled-throwable"))

(def example-error-routes
  [["/send-error-code" example-route-send-error-code-handler]
   ["/throw" example-route-throw-handler]
   ["/error-code" example-route-error-code-handler]
   ["/error-throwable" example-route-throwable-handler]])

(def example-error-pages
  {500                        "/error-code"
   clojure.lang.ExceptionInfo "/error-throwable"})

(deftest test-error-pages
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    {:routes      example-error-routes
                             :error-pages example-error-pages}}
    (let [resp (send "http://localhost:42000/send-error-code")]
      (is (= (:status resp) 500))
      (is (= (:body resp) "handled-error-code")))
    (let [resp (send "http://localhost:42000/throw")]
      (is (= (:status resp) 500))
      (is (= (:body resp) "handled-throwable")))))

#_(deftest test-gzip-handler
    (with-config {:connectors [{:protocol :http :port 42000}]
                  :handler    {:routes       [["/*" (constantly {:serval.response/body "foo"})]]
                               :gzip-handler {:min-gzip-size 0}}}
      (let [req  {:uri     "http://localhost:42000"
                  :headers {:accept-encoding "gzip"}}
            resp (send req :input-stream)]
        (is (= (:status resp) 200))
        (is (= (slurp (GZIPInputStream. (:body resp))) "foo")))))

(def example-handler-instance
  ;; https://eclipse.dev/jetty/javadoc/jetty-12/org/eclipse/jetty/server/Request.html
  (proxy [Handler$Abstract] []
    (handle [request ^Response response ^Callback callback]
      (.setStatus response 200)
      (.write response true (java.nio.ByteBuffer/wrap (.getBytes "foo")) callback)
      (.succeeded callback)
      true)))

(deftest test-handler-instance
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    example-handler-instance}
    (let [resp (send "http://localhost:42000")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))
