(ns dev.onionpancakes.serval.tests.jetty.test-jetty
  (:require [dev.onionpancakes.serval.jetty :as sj]
            [clojure.test :refer [deftest is]])
  (:import [jakarta.servlet Servlet]
           [org.eclipse.jetty.server
            Server ServerConnector
            HttpConnectionFactory]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]
           [org.eclipse.jetty.servlet ServletContextHandler]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]))

(deftest test-http-configuration
  (let [conf {:send-date-header?    false
              :send-server-version? false
              :request-header-size  123
              :response-header-size 123
              :output-buffer-size   123}
        hconf (sj/http-configuration conf)]
    (is (= false (.getSendDateHeader hconf)))
    (is (= false (.getSendServerVersion hconf)))
    (is (= 123 (.getRequestHeaderSize hconf)))
    (is (= 123 (.getResponseHeaderSize hconf)))
    (is (= 123 (.getOutputBufferSize hconf)))))

(deftest test-configure-connector
  ;; Http
  (let [server (Server.)
        conn   (ServerConnector. server)
        conf   {:protocol     :http
                :port         3000
                :host         "0.1.2.3"
                :idle-timeout 1234}
        _      (sj/configure-connector! conn conf)]
    (is (= 3000 (.getPort conn)))
    (is (= "0.1.2.3" (.getHost conn)))
    (is (= 1234 (.getIdleTimeout conn)))
    (is (instance? HttpConnectionFactory (first (.getConnectionFactories conn)))))

  ;; Http2 clear text
  (let [server (Server.)
        conn   (ServerConnector. server)
        conf   {:protocol :http2c}
        _      (sj/configure-connector! conn conf)
        facts  (.getConnectionFactories conn)]
    (is (instance? HttpConnectionFactory (first facts)))
    (is (instance? HTTP2CServerConnectionFactory (second facts)))))

(deftest test-multipart-config
  (let [conf  {:location            "/tmp"
               :max-file-size       123
               :max-request-size    123
               :file-size-threshold 123}
        mpart (sj/multipart-config conf)]
    (is (= "/tmp" (.getLocation mpart)))
    (is (= 123 (.getMaxFileSize mpart)))
    (is (= 123 (.getMaxRequestSize mpart)))
    (is (= 123 (.getFileSizeThreshold mpart))))

  ;; Conf missing location throws.
  (is (thrown? clojure.lang.ExceptionInfo (sj/multipart-config {}))))

(deftest test-servlet-context-handler
  ;; Test via servlet registration.
  (let [servlet  (reify Servlet
                   (service [this _ _]))
        servlet2 (reify Servlet
                   (service [this _ _]))
        conf     [["/*" servlet]
                  ["/foo" servlet2]]
        handler  (sj/servlet-context-handler conf)

        ;; Mapping from typename string to paths set.
        type2paths (->> (.. handler getServletContext)
                        (.getServletRegistrations)
                        (vals)
                        (map (juxt (memfn getClassName)
                                   (comp set (memfn getMappings))))
                        (into {}))]
    (is (= {(.getTypeName (type servlet))  #{"/*"}
            (.getTypeName (type servlet2)) #{"/foo"}} type2paths))))

(deftest test-gzip-handler
  (let [conf {:excluded-methods    ["GET" "POST"]
              :excluded-mime-types ["text/plain" "application/json"]
              :excluded-paths      ["/foo" "/bar"]
              :included-methods    ["HEAD" "PATCH"]
              :included-mime-types ["text/html" "text/css"]
              :included-paths      ["/buz" "/baz"]
              :min-gzip-size       123}
        gzip (sj/gzip-handler conf)]
    (is (= #{"GET" "POST"} (set (.getExcludedMethods gzip))))
    (is (= #{"text/plain" "application/json"} (set (.getExcludedMimeTypes gzip))))
    (is (= #{"/foo" "/bar"} (set (.getExcludedPaths gzip))))
    (is (= #{"HEAD" "PATCH"} (set (.getIncludedMethods gzip))))
    (is (= #{"text/html" "text/css"} (set (.getIncludedMimeTypes gzip))))
    (is (= #{"/buz" "/baz"} (set (.getIncludedPaths gzip))))
    (is (= 123 (.getMinGzipSize gzip)))))

(deftest test-handler-tree
  (let [servlet (reify Servlet
                  (service [this _ _]))
        conf    {:servlets [["/*" servlet]]
                 :gzip     true}
        tree    (sj/handler-tree conf)]
    (is (instance? GzipHandler tree))
    (is (instance? ServletContextHandler (.getHandler tree)))))

(deftest test-thread-pool
  (let [pool (sj/thread-pool {:min-threads  1
                              :max-threads  8
                              :idle-timeout 1000})]
    (is (= 1 (.getMinThreads pool)))
    (is (= 8 (.getMaxThreads pool)))
    (is (= 1000 (.getIdleTimeout pool)))))

(deftest test-server
  (let [servlet (reify Servlet
                  (service [this _ _]))
        conf    {:thread-pool {:min-threads 1
                               :max-threads 8}
                 :servlets    [["/*" servlet]]
                 :gzip        true}
        server  (sj/server conf)
        pool    (.getThreadPool server)
        tree    (.getHandler server)
        sctx    (.getHandler tree)]
    (is (= 1 (.getMinThreads pool)))
    (is (= 8 (.getMaxThreads pool)))
    (is (instance? GzipHandler tree))
    (is (instance? ServletContextHandler sctx))))
