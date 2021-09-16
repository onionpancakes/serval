(ns dev.oniopancakes.serval.tests.jetty.test-jetty
  (:require [dev.onionpancakes.serval.jetty :as sj]
            [clojure.test :refer [deftest is]])
  (:import [org.eclipse.jetty.server
            Server ServerConnector
            HttpConnectionFactory]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]))

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
