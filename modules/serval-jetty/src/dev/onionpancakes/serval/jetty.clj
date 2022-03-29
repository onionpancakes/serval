(ns dev.onionpancakes.serval.jetty
  (:require [dev.onionpancakes.serval.core :as serval])
  (:import [jakarta.servlet Servlet]
           [org.eclipse.jetty.server
            Server Handler ServerConnector
            HttpConnectionFactory HttpConfiguration
            CustomRequestLog]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]
           [org.eclipse.jetty.servlet ServletHolder ServletContextHandler]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]
           [org.eclipse.jetty.util.thread ThreadPool QueuedThreadPool]))

;; Servlets and handlers

(defprotocol IServlet
  (to-servlet ^Servlet [this]))

(extend-protocol IServlet
  clojure.lang.AFunction
  (to-servlet [this]
    (serval/http-servlet this))
  clojure.lang.Var
  (to-servlet [this]
    (serval/http-servlet this))
  Servlet
  (to-servlet [this] this))

(defn ^ServletHolder servlet-holder
  [servlet config]
  (let [holder (ServletHolder. (to-servlet servlet))]
    (if (contains? config :multipart-config)
      (-> (.getRegistration holder)
          (.setMultipartConfig (:multipart-config config))))
    holder))

(defn servlet-context-handler
  [servlet-context-spec]
  (let [sch (ServletContextHandler.)]
    (doseq [[^String path servlet config] servlet-context-spec]
      (.addServlet sch (servlet-holder servlet config) path))
    sch))

(defprotocol IHandler
  (to-handler [this]))

(extend-protocol IHandler
  clojure.lang.PersistentVector
  (to-handler [this]
    (servlet-context-handler this))
  clojure.lang.AFunction
  (to-handler [this]
    (servlet-context-handler [["/*" this]]))
  clojure.lang.Var
  (to-handler [this]
    (servlet-context-handler [["/*" this]]))
  Servlet
  (to-handler [this]
    (servlet-context-handler [["/*" this]]))
  Handler
  (to-handler [this] this))

(defn ^GzipHandler gzip-handler
  ([handler] (gzip-handler handler nil))
  ([handler config]
   (let [gzhandler (GzipHandler.)]
     (.setHandler gzhandler (to-handler handler))
     (if (contains? config :excluded-methods)
       (->> (:excluded-methods config)
            (map name)
            (into-array String)
            (.setExcludedMethods gzhandler)))
     (if (contains? config :excluded-mime-types)
       (->> (:excluded-mime-types config)
            (into-array String)
            (.setExcludedMimeTypes gzhandler)))
     (if (contains? config :excluded-paths)
       (->> (:excluded-paths config)
            (into-array String)
            (.setExcludedPaths gzhandler)))
     (if (contains? config :included-methods)
       (->> (:included-methods config)
            (map name)
            (into-array String)
            (.setIncludedMethods gzhandler)))
     (if (contains? config :included-mime-types)
       (->> (:included-mime-types config)
            (into-array String)
            (.setIncludedMimeTypes gzhandler)))
     (if (contains? config :included-paths)
       (->> (:included-paths config)
            (into-array String)
            (.setIncludedPaths gzhandler)))
     (if (contains? config :min-gzip-size)
       (.setMinGzipSize gzhandler (:min-gzip-size config)))
     gzhandler)))

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

(defmethod connection-factories nil
  [config]
  (let [hconf (http-configuration config)
        http1 (HttpConnectionFactory. hconf)]
    [http1]))

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

(defn ^QueuedThreadPool queued-thread-pool
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

(defn ^Server configure-server!
  [^Server server config]
  (if (contains? config :connectors)
    (->> (:connectors config)
         (map (partial server-connector server))
         (into-array ServerConnector)
         (.setConnectors server)))
  (if (contains? config :handler)
    (.setHandler server (to-handler (:handler config))))
  (if (contains? config :request-log)
    (.setRequestLog server (:request-log config)))
  server)

(defn ^Server server*
  ([config]
   (doto (Server.)
     (configure-server! config)))
  ([^ThreadPool pool config]
   (doto (Server. pool)
     (configure-server! config))))

(def default-server-config
  {:connectors  [{:protocol :http :port 3000}]
   :handler     (fn [ctx]
                  {:serval.response/body "Hello world!"})
   :request-log (CustomRequestLog.)})

(defn ^Server server
  ([config]
   (server* (merge default-server-config config)))
  ([pool config]
   (server* pool (merge default-server-config config))))
