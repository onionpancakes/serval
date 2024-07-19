(ns dev.onionpancakes.serval.jetty.impl.server
  (:require [dev.onionpancakes.serval.jetty.impl.handlers :as impl.handlers])
  (:import [org.eclipse.jetty.server
            Handler
            HttpConfiguration
            HttpConnectionFactory
            Server
            ServerConnector]
           [org.eclipse.jetty.server.handler ContextHandler]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]
           [org.eclipse.jetty.util.thread ThreadPool]))

(defprotocol ServerContextHandler
  (^ContextHandler as-context-handler [this]))

(defprotocol ServerHandler
  (^Handler as-handler [this]))

(def ^:dynamic *default-context-handler-fn*
  'dev.onionpancakes.serval.jetty.impl.ee10.servlet/servlet-context-handler)

(extend-protocol ServerContextHandler
  clojure.lang.IPersistentVector
  (as-context-handler [[path handler :as this]]
    (when-not (== (count this) 2)
      (throw (IllegalArgumentException. "Context route vector must be [path handler] pair.")))
    (doto (as-context-handler handler)
      (.setContextPath path)))
  clojure.lang.IPersistentMap
  (as-context-handler [this]
    (let [context-handler-fn (requiring-resolve *default-context-handler-fn*)]
      (context-handler-fn this)))
  ContextHandler
  (as-context-handler [this] this)
  Object
  (as-context-handler [this]
    (as-context-handler {:routes [["/*" this]]})))

(extend-protocol ServerHandler
  clojure.lang.IPersistentMap
  (as-handler [this]
    (as-context-handler this))
  java.util.List
  (as-handler [this]
    (let [handlers (mapv as-context-handler this)]
      (impl.handlers/context-handler-collection {:handlers handlers})))
  Handler
  (as-handler [this] this)
  Object
  (as-handler [this]
    (as-context-handler this))
  nil
  (as-server-handler [_] nil))

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

;; Server

(defn configure-server
  [^Server server config]
  (when (contains? config :connectors)
    (->> (:connectors config)
         (map (partial server-connector server))
         (into-array ServerConnector)
         (.setConnectors server)))
  (when (contains? config :handler)
    (.setHandler server (as-handler (:handler config))))
  (when (contains? config :request-log)
    (.setRequestLog server (:request-log config)))
  server)

(defn server
  (^Server []
   (Server.))
  (^Server [^ThreadPool thread-pool]
   (Server. thread-pool))
  (^Server [^ThreadPool thread-pool scheduler buffer-pool]
   (Server. thread-pool scheduler buffer-pool)))
