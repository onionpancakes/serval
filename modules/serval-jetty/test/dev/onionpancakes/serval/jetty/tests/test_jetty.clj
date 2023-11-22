(ns dev.onionpancakes.serval.jetty.tests.test-jetty
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core
             :refer [response]]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-config send]]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [clojure.test :refer [is deftest]])
  (:import [jakarta.servlet.http HttpServletResponse]
           [java.net.http HttpClient$Version]
           [java.util.zip GZIPInputStream]
           [org.eclipse.jetty.server Handler$Abstract Response]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]
           [org.eclipse.jetty.util Callback VirtualThreads]))

(deftest test-minimal
  (with-config {:connectors [{:port 42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (:version resp) HttpClient$Version/HTTP_1_1))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-http1
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (:version resp) HttpClient$Version/HTTP_1_1))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-http2c
  (with-config {:connectors [{:protocol :http2c :port 42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (:version resp) HttpClient$Version/HTTP_2))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-multiple-connectors
  (with-config {:connectors [{:protocol :http :port 42000}
                             {:protocol :http2c :port 42001}]
                :handler    #(case (long (:server-port (:serval.context/request %)))
                               42000 {:serval.response/body "foo"}
                               42001 {:serval.response/body "bar"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (:version resp) HttpClient$Version/HTTP_1_1))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42001")]
      (is (= (:version resp) HttpClient$Version/HTTP_2))
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
  [{:serval.context/keys [request ^HttpServletResponse response] :as ctx}]
  (let [error-code (-> (:path-info request)
                       (subs 1)
                       (parse-long))]
    (.sendError response error-code))
  ctx)

(defn example-route-throw-handler
  [ctx]
  (throw (ex-info "foobar" {})))

(defn example-route-error-code-handler
  [ctx]
  (response ctx 500 "handled-error-code"))

(defn example-route-error-code-range-handler
  [ctx]
  (response ctx 500 "handled-error-code-range"))

(defn example-route-throwable-handler
  [ctx]
  (response ctx 500 "handled-throwable"))

(def example-error-routes
  [["/send-error-code/*" example-route-send-error-code-handler]
   ["/throw" example-route-throw-handler]
   ["/error-code" example-route-error-code-handler]
   ["/error-code-range" example-route-error-code-range-handler]
   ["/error-throwable" example-route-throwable-handler]])

(def example-error-pages
  {500                        "/error-code"
   [400 499]                  "/error-code-range"
   clojure.lang.ExceptionInfo "/error-throwable"})

(deftest test-error-pages
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    {:routes      example-error-routes
                             :error-pages example-error-pages}}
    (let [resp (send "http://localhost:42000/send-error-code/500")]
      (is (= (:status resp) 500))
      (is (= (:body resp) "handled-error-code")))
    (let [resp (send "http://localhost:42000/send-error-code/400")]
      (is (= (:status resp) 500))
      (is (= (:body resp) "handled-error-code-range")))
    (let [resp (send "http://localhost:42000/send-error-code/404")]
      (is (= (:status resp) 500))
      (is (= (:body resp) "handled-error-code-range")))
    (let [resp (send "http://localhost:42000/send-error-code/499")]
      (is (= (:status resp) 500))
      (is (= (:body resp) "handled-error-code-range")))
    (let [resp (send "http://localhost:42000/throw")]
      (is (= (:status resp) 500))
      (is (= (:body resp) "handled-throwable")))))

(def example-gzip-body
  (->> (repeat "foobarbaz")
       (take 10)
       (apply str)))

(defn example-gzip-route-handler
  [ctx]
  (response ctx 200 example-gzip-body))

(deftest test-gzip-handler
  ;; With true
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    {:routes       [["/*" example-gzip-route-handler]]
                             :gzip-handler true}}
    (let [req  {:uri     "http://localhost:42000"
                :headers {:accept-encoding "gzip"}}
          resp (send req :input-stream)]
      (is (= (:status resp) 200))
      (is (= (slurp (GZIPInputStream. (:body resp))) example-gzip-body))))
  ;; With false
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    {:routes       [["/*" example-gzip-route-handler]]
                             :gzip-handler false}}
    (let [req  {:uri     "http://localhost:42000"
                :headers {:accept-encoding "gzip"}}
          resp (send req :input-stream)]
      (is (= (:status resp) 200))
      (is (= (slurp (:body resp)) example-gzip-body))))
  ;; With config map
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    {:routes       [["/*" example-gzip-route-handler]]
                             :gzip-handler {:min-gzip-size 0}}}
    (let [req  {:uri     "http://localhost:42000"
                :headers {:accept-encoding "gzip"}}
          resp (send req :input-stream)]
      (is (= (:status resp) 200))
      (is (= (slurp (GZIPInputStream. (:body resp))) example-gzip-body))))
  ;; With direct GzipHandler instance.
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    {:routes       [["/*" example-gzip-route-handler]]
                             :gzip-handler (doto (GzipHandler.)
                                             (.setMinGzipSize 0))}}
    (let [req  {:uri     "http://localhost:42000"
                :headers {:accept-encoding "gzip"}}
          resp (send req :input-stream)]
      (is (= (:status resp) 200))
      (is (= (slurp (GZIPInputStream. (:body resp))) example-gzip-body)))))

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

(deftest test-multiple-context
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    (srv.jetty/server-handler
                             {:context-path "/foo"
                              :routes       [["/*" (constantly {:serval.response/status 200
                                                                :serval.response/body   "foo"})]]}
                             {:context-path "/bar"
                              :routes       [["/*" (constantly {:serval.response/status 200
                                                                :serval.response/body   "bar"})]]})}
    (let [resp (send "http://localhost:42000/foo")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42000/bar")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))))

(deftest test-queued-thread-pool
  (let [config {:min-threads     1
                :max-threads     2
                :idle-timeout    1000}
        pool   (srv.jetty/queued-thread-pool config)]
    (is (= (.getMinThreads pool) 1))
    (is (= (.getMaxThreads pool) 2))
    (is (= (.getIdleTimeout pool) 1000))))

(deftest test-queued-thread-pool-virtual-threads
  (when (VirtualThreads/areSupported)
    (let [config {:virtual-threads true}
          pool   (srv.jetty/queued-thread-pool config)]
      (is (some? (.getVirtualThreadsExecutor pool))))
    (let [config {:virtual-threads (VirtualThreads/getDefaultVirtualThreadsExecutor)}
          pool   (srv.jetty/queued-thread-pool config)]
      (is (some? (.getVirtualThreadsExecutor pool))))
    (let [config {:virtual-threads false}
          pool   (srv.jetty/queued-thread-pool config)]
      (is (nil? (.getVirtualThreadsExecutor pool))))
    (let [config {:virtual-threads nil}
          pool   (srv.jetty/queued-thread-pool config)]
      (is (nil? (.getVirtualThreadsExecutor pool))))))
