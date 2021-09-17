(ns dev.onionpancakes.serval.jetty
  (:import [jakarta.servlet Servlet MultipartConfigElement]
           [org.eclipse.jetty.server
            Server Handler ServerConnector
            ConnectionFactory HttpConnectionFactory HttpConfiguration
            CustomRequestLog]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]
           [org.eclipse.jetty.servlet ServletHolder ServletContextHandler]
           [org.eclipse.jetty.server.handler HandlerWrapper]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]
           [org.eclipse.jetty.util.thread ThreadPool QueuedThreadPool]))

;; Connectors

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

(defn configure-connector!
  [^ServerConnector conn config]
  (.setConnectionFactories conn (connection-factories config))
  (if (contains? config :port)
    (.setPort conn (:port config)))
  (if (contains? config :host)
    (.setHost conn (:host config)))
  (if (contains? config :idle-timeout)
    (.setIdleTimeout conn (:idle-timeout config))))

;; Handler

(defn multipart-config
  [{:keys [location max-file-size max-request-size
           file-size-threshold]
    :or   {max-file-size       -1
           max-request-size    -1
           file-size-threshold 0}}]
  (try
    (assert location)
    (catch AssertionError e
      (throw (ex-info "Multipart config missing location key." {}))))
  
  (MultipartConfigElement. location max-file-size max-request-size
                           file-size-threshold))

(defn ^ServletHolder servlet-holder
  [^Servlet servlet opts]
  (let [holder (ServletHolder. servlet)]
    (if-let [mconf (:multipart opts)]
      (-> (.getRegistration holder)
          (.setMultipartConfig (multipart-config mconf))))
    holder))

(defn servlet-context-handler
  [path-to-servlets]
  (let [^ServletContextHandler sch (ServletContextHandler.)]
    (doseq [[^String path ^Servlet serv opts] path-to-servlets]
      (.addServlet sch (servlet-holder serv opts) path))
    sch))

(defn gzip-handler
  [config]
  (let [handler (GzipHandler.)]
    (when-let [methods (:excluded-methods config)]
      (->> (map name methods)
           (into-array String)
           (.setExcludedMethods handler)))
    (when-let [mime-types (:excluded-mime-types config)]
      (->> (into-array String mime-types)
           (.setExcludedMimeTypes handler)))
    (when-let [paths (:excluded-paths config)]
      (->> (into-array String paths)
           (.setExcludedPaths handler)))
    (when-let [methods (:included-methods config)]
      (->> (map name methods)
           (into-array String)
           (.setIncludedMethods handler)))
    (when-let [mime-types (:included-mime-types config)]
      (->> (into-array String mime-types)
           (.setIncludedMimeTypes handler)))
    (when-let [paths (:included-paths config)]
      (->> (into-array String paths)
           (.setIncludedPaths handler)))
    (when-let [size (:min-gzip-size config)]
      (.setMinGzipSize handler size))
    handler))

(defn wrap-handler!
  [handler ^HandlerWrapper wrapping]
  (doto wrapping
    (.setHandler handler)))

(defn handler-tree
  [config]
  ;; TODO: Warn if :servlets is missing, but :gzip is specified?
  (if (:servlets config)
    (cond-> (servlet-context-handler (:servlets config))
      (:gzip config) (wrap-handler! (gzip-handler (:gzip config))))))

;; Server

(defn thread-pool
  [config]
  (let [pool (QueuedThreadPool.)]
    (when-let [min-threads (:min-threads config)]
      (.setMinThreads pool min-threads))
    (when-let [max-threads (:max-threads config)]
      (.setMaxThreads pool max-threads))
    (when-let [timeout (:idle-timeout config)]
      (.setIdleTimeout pool timeout))
    pool))

(defn configure-server!
  [^Server server config]
  (->> (:connectors config)
       (map #(doto (ServerConnector. server)
               (configure-connector! %)))
       (into-array ServerConnector)
       (.setConnectors server))
  (.setHandler server (handler-tree config))
  (.setRequestLog server (CustomRequestLog.)))

(defn server
  [config]
  (doto (if (:thread-pool config)
          (Server. ^ThreadPool (thread-pool (:thread-pool config)))
          (Server.))
    (configure-server! config)))
