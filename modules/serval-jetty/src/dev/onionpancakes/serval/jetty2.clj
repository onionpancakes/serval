(ns dev.onionpancakes.serval.jetty2
  (:import [org.eclipse.jetty.server
            Server Handler ServerConnector
            ConnectionFactory HttpConnectionFactory HttpConfiguration
            CustomRequestLog]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]
           [org.eclipse.jetty.servlet ServletHolder ServletContextHandler]
           [org.eclipse.jetty.server.handler HandlerWrapper]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]
           [org.eclipse.jetty.util.thread ThreadPool QueuedThreadPool]))

;; Server connector

(defn http-configuration
  [config]
  (let [hconf (HttpConfiguration.)]
    (if (contains? config :send-date-header?)
      (.setSendDateHeader hconf (:send-date-header? config)))
    (if (contains? config :send-server-version?)
      (.setSendServerVersion hconf (:send-server-version? config)))
    (if (contains? config :request-header-size)
      (.setRequestHeaderSize hconf (:request-header-size config)))
    (if (contains? config :response-header-size)
      (.setResponseHeaderSize hconf (:response-header-size config)))
    (if (contains? config :output-buffer-size)
      (.setOutputBufferSize hconf (:output-buffer-size config)))
    hconf))

(defmulti connection-factories :protocol)

(defmethod connection-factories :http
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

(defn server-connector
  [server config]
  (let [conn (ServerConnector. server)]
    (if (contains? config :protocol)
      (.setConnectionFactories conn (connection-factories config)))
    (if (contains? config :port)
      (.setPort conn (:port config)))
    (if (contains? config :host)
      (.setHost conn (:host config)))
    (if (contains? config :idle-timeout)
      (.setIdleTimeout conn (:idle-timeout config)))
    conn))

;; Thread pool

(defn queued-thread-pool
  [config]
  (let [pool (QueuedThreadPool.)]
    (if (contains? config :min-threads)
      (.setMinThreads pool (:min-threads config)))
    (if (contains? config :max-threads)
      (.setMaxThreads pool (:max-threads config)))
    (if (contains? config :idle-timeout)
      (.setIdleTimeout pool (:idle-timeout config)))
    pool))

;; Server

(defn configure-server!
  [^Server server config]
  (if (contains? config :connectors)
    (->> (:connectors config)
         (map (partial server-connector server))
         (into-array ServerConnector)
         (.setConnectors server)))
  (if (contains? config :handler)
    (.setHandler server (:handler config)))
  (if (contains? config :request-log)
    (.setRequestLog server (:request-log config))))

(defn server*
  ([config]
   (doto (Server.)
     (configure-server! config)))
  ([^ThreadPool pool config]
   (doto (Server. pool)
     (configure-server! config))))
