(ns dev.onionpancakes.serval.jetty.tests.test-jetty
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty-test
             :refer [with-config send]]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [clojure.test :refer [is deftest]])
  (:import [jakarta.servlet.http HttpServletResponse]
           [java.net.http HttpClient$Version]
           [java.util.zip GZIPInputStream]
           [org.eclipse.jetty.server Handler$Abstract Response]
           [org.eclipse.jetty.util Callback VirtualThreads]))

(deftest test-minimal
  (with-config {:connectors [{:port 42000}]
                :handler    (fn [_ _ response]
                              (srv/write-body response "foo"))}
    (let [resp (send "http://localhost:42000")]
      (is (= (:version resp) HttpClient$Version/HTTP_1_1))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-http1
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    (fn [_ _ response]
                              (srv/write-body response "foo"))}
    (let [resp (send "http://localhost:42000")]
      (is (= (:version resp) HttpClient$Version/HTTP_1_1))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-http2c
  (with-config {:connectors [{:protocol :http2c :port 42000}]
                :handler    (fn [_ _ response]
                              (srv/write-body response "foo"))}
    (let [resp (send "http://localhost:42000")]
      (is (= (:version resp) HttpClient$Version/HTTP_2))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(deftest test-multiple-connectors
  (with-config {:connectors [{:protocol :http :port 42000}
                             {:protocol :http2c :port 42001}]
                :handler    (fn [_ request response]
                              (case (long (:server-port request))
                                42000 (srv/write-body response "foo")
                                42001 (srv/write-body response "bar")))}
    (let [resp (send "http://localhost:42000")]
      (is (= (:version resp) HttpClient$Version/HTTP_1_1))
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42001")]
      (is (= (:version resp) HttpClient$Version/HTTP_2))
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))))

(defn example-var-handler
  [_ _ response]
  (srv/write-body response "foo"))

(deftest test-var-handler
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    #'example-var-handler}
    (let [resp (send "http://localhost:42000")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))))

(def example-routes
  [["/foo" (fn [_ _ response]
             (srv/write-body response "foo"))]
   ["/bar" (fn [_ _ response]
             (srv/write-body response "bar"))]
   ["/*"   (fn [_ _ response]
             (srv/write-body response "default"))]])

(deftest test-routes-handler
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
  [_ request response]
  (let [error-code (-> (:path-info request)
                       (subs 1)
                       (parse-long))]
    (srv/send-error response error-code)))

(defn example-route-throw-handler
  [_ _ _]
  (throw (ex-info "foobar" {})))

(defn example-route-error-code-handler
  [_ _ response]
  (doto response
    (srv/set-http :status 500)
    (srv/write-body "handled-error-code")))

(defn example-route-error-code-range-handler
  [_ _ response]
  (doto response
    (srv/set-http :status 500)
    (srv/write-body "handled-error-code-range")))

(defn example-route-throwable-handler
  [_ _ response]
  (doto response
    (srv/set-http :status 500)
    (srv/write-body "handled-throwable")))

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
  [_ _ response]
  (doto response
    (srv/write-body example-gzip-body)))

(deftest test-gzip-handler
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    {:routes   [["/*" example-gzip-route-handler]]
                             :handlers [(srv.jetty/gzip-handler :min-gzip-size 0)]}}
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

(deftest test-server-handler-multiple-context
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    [{:context-path "/foo"
                              :routes       [["/*" (fn [_ _ response]
                                                     (srv/write-body response "foo"))]]}
                             {:context-path "/bar"
                              :routes       [["/*" (fn [_ _ response]
                                                     (srv/write-body response "bar"))]]}]}
    (let [resp (send "http://localhost:42000/foo")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42000/bar")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))))

(deftest test-server-handler-context-routes
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    [["/foo" {:routes [["/*" (fn [_ _ response]
                                                       (srv/write-body response "foo"))]]}]
                             ["/bar" {:routes [["/*" (fn [_ _ response]
                                                       (srv/write-body response "bar"))]]}]
                             ["/baz" {:context-route "/foo"
                                      :routes        [["/*" (fn [_ _ response]
                                                              (srv/write-body response "baz"))]]}]]}
    (let [resp (send "http://localhost:42000/foo")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42000/bar")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))
    (let [resp (send "http://localhost:42000/baz")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "baz")))))

(deftest test-server-handler-multiple-handlers-mixed
  (with-config {:connectors [{:protocol :http :port 42000}]
                :handler    [(fn [_ _ response]
                               (srv/write-body response "foo"))
                             {:context-path "/bar"
                              :routes       [["/*" (fn [_ _ response]
                                                     (srv/write-body response "bar"))]]}
                             ["/baz" {:routes [["/*" (fn [_ _ response]
                                                       (srv/write-body response "baz"))]]}]]}
    (let [resp (send "http://localhost:42000")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "foo")))
    (let [resp (send "http://localhost:42000/bar")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "bar")))
    (let [resp (send "http://localhost:42000/baz")]
      (is (= (:status resp) 200))
      (is (= (:body resp) "baz")))))

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
