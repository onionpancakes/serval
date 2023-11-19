(ns dev.onionpancakes.serval.jetty.impl.server
  (:require [dev.onionpancakes.serval.jetty.impl.handlers :as impl.handlers]
            [dev.onionpancakes.serval.jetty.impl.protocols :as p])
  (:import [org.eclipse.jetty.server
            Server Handler ServerConnector
            HttpConnectionFactory HttpConfiguration
            CustomRequestLog]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]
           [org.eclipse.jetty.util.thread ThreadPool]))

;; ServerConnector

(defn http-configuration
  [config]
  (let [hconf (HttpConfiguration.)]
    (when (contains? config :send-date-header?)
      (.setSendDateHeader hconf (:send-date-header? config)))
    (when (contains? config :send-server-version?)
      (.setSendServerVersion hconf (:send-server-version? config)))
    (when (contains? config :request-header-size)
      (.setRequestHeaderSize hconf (:request-header-size config)))
    (when (contains? config :response-header-size)
      (.setResponseHeaderSize hconf (:response-header-size config)))
    (when (contains? config :output-buffer-size)
      (.setOutputBufferSize hconf (:output-buffer-size config)))
    hconf))

(defmulti connection-factories :protocol)

(defmethod connection-factories :http
  [config]
  (let [hconf (http-configuration config)
        http1 (HttpConnectionFactory. hconf)]
    [http1]))

(defmethod connection-factories :http1.1
  [config]
  (let [hconf (http-configuration config)
        http1 (HttpConnectionFactory. hconf)]
    [http1]))

(defmethod connection-factories :http2c
  [config]
  (let [hconf (http-configuration config)
        http1 (HttpConnectionFactory. hconf)
        http2 (HTTP2CServerConnectionFactory. hconf)]
    [http1 http2]))

(defmethod connection-factories nil
  [config]
  (let [hconf (http-configuration config)
        http1 (HttpConnectionFactory. hconf)]
    [http1]))

(defn server-connector
  [server config]
  (let [conn (ServerConnector. server)]
    (when (contains? config :protocol)
      (.setConnectionFactories conn (connection-factories config)))
    (when (contains? config :port)
      (.setPort conn (:port config)))
    (when (contains? config :host)
      (.setHost conn (:host config)))
    (when (contains? config :idle-timeout)
      (.setIdleTimeout conn (:idle-timeout config)))
    conn))

;; ServerHandler

(def ^:dynamic *server-handler*
  'dev.onionpancakes.serval.jetty.impl.ee10.servlet/as-servlet-context-handler)

(defn as-server-handler
  [config]
  (let [server-handler (requiring-resolve *server-handler*)]
    (server-handler config)))

(extend-protocol p/ServerHandler
  Handler
  (as-server-handler [this] this)
  Object
  (as-server-handler [this]
    (as-server-handler this))
  nil
  (as-server-handler [this]
    (as-server-handler nil)))

;; Server

(defn configure-server
  [^Server server config]
  (when (contains? config :connectors)
    (->> (:connectors config)
         (map (partial server-connector server))
         (into-array ServerConnector)
         (.setConnectors server)))
  (when (contains? config :handler)
    (.setHandler server (p/as-server-handler (:handler config))))
  (when (contains? config :request-log)
    (.setRequestLog server (:request-log config)))
  server)

(defn server
  (^Server []
   (Server.))
  (^Server [^ThreadPool pool]
   (Server. pool)))
