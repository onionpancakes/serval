(ns dev.onionpancakes.serval.jetty
  (:refer-clojure :exclude [error-handler])
  (:require [dev.onionpancakes.serval.core :as serval]
            [dev.onionpancakes.serval.io.http :as io.http])
  (:import [jakarta.servlet Servlet]
           [org.eclipse.jetty.server
            Server Handler ServerConnector
            HttpConnectionFactory HttpConfiguration
            CustomRequestLog]
           [org.eclipse.jetty.server.handler ErrorHandler]
           [org.eclipse.jetty.server.session SessionHandler]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]
           [org.eclipse.jetty.servlet ServletHolder ServletContextHandler]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]
           [org.eclipse.jetty.util.thread ThreadPool QueuedThreadPool]))

;; Servlet

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

;; ServletHolder

(defn servlet-holder
  [{:keys [servlet name multipart-config]}]
  (let [holder (ServletHolder.)]
    (if servlet
      (.setServlet holder (to-servlet servlet)))
    (if name
      (.setName holder name))
    (if multipart-config
      (-> (.getRegistration holder)
          (.setMultipartConfig multipart-config)))
    holder))

(defprotocol IServletHolder
  (to-servlet-holder [this]))

(extend-protocol IServletHolder
  java.util.Map
  (to-servlet-holder [this]
    (servlet-holder this))
  clojure.lang.AFunction
  (to-servlet-holder [this]
    (servlet-holder {:servlet this}))
  clojure.lang.Var
  (to-servlet-holder [this]
    (servlet-holder {:servlet this}))
  Servlet
  (to-servlet-holder [this]
    (servlet-holder {:servlet this}))
  ServletHolder
  (to-servlet-holder [this] this))

;; SessionHandler

(defprotocol ISessionHandler
  (to-session-handler [this]))

(extend-protocol ISessionHandler
  SessionHandler
  (to-session-handler [this] this))

;; ErrorHandler

(defn error-handler
  [handler-fn]
  (let [service-fn (io.http/service-fn handler-fn)]
    (proxy [ErrorHandler] []
      (handle [_ _ request response]
        (service-fn this request response)))))

(defprotocol IErrorHandler
  (to-error-handler [this]))

(extend-protocol IErrorHandler
  clojure.lang.AFunction
  (to-error-handler [this]
    (error-handler this))
  clojure.lang.Var
  (to-error-handler [this]
    (error-handler this))
  ErrorHandler
  (to-error-handler [this] this))

;; GzipHandler

(defn ^GzipHandler gzip-handler
  ([] (gzip-handler nil))
  ([config]
   (let [gzhandler (GzipHandler.)]
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

(defprotocol IGzipHandler
  (to-gzip-handler [this]))

(extend-protocol IGzipHandler
  java.util.Map
  (to-gzip-handler [this]
    (gzip-handler this))
  Boolean
  (to-gzip-handler [this]
    (if this (gzip-handler) nil))
  Handler
  (to-gzip-handler [this] this))

;; ServletContextHandler

(defprotocol IServletPaths
  (servlet-paths [this]))

(extend-protocol IServletPaths
  clojure.lang.PersistentVector
  (servlet-paths [this] this)
  clojure.lang.AFunction
  (servlet-paths [this] [["/*" this]])
  clojure.lang.Var
  (servlet-paths [this] [["/*" this]])
  Servlet
  (servlet-paths [this] [["/*" this]])
  nil
  (servlet-paths [this] nil))

(defn servlet-context-handler
  [{:keys [servlets session-handler error-handler gzip-handler]}]
  (let [sch (ServletContextHandler.)]
    (doseq [[^String path srv-holder] (servlet-paths servlets)]
      (.addServlet sch (to-servlet-holder srv-holder) path))
    (if session-handler
      (.setSessionHandler sch (to-session-handler session-handler)))
    (if error-handler
      (.setErrorHandler sch (to-error-handler error-handler)))
    (if gzip-handler
      (.insertHandler sch (to-gzip-handler gzip-handler)))
    sch))

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

(defprotocol IServerHandler
  (to-server-handler [this]))

(extend-protocol IServerHandler
  java.util.Map
  (to-server-handler [this]
    (servlet-context-handler this))
  clojure.lang.PersistentVector
  (to-server-handler [this]
    (servlet-context-handler {:servlets this}))
  clojure.lang.AFunction
  (to-server-handler [this]
    (servlet-context-handler {:servlets this}))
  clojure.lang.Var
  (to-server-handler [this]
    (servlet-context-handler {:servlets this}))
  Servlet
  (to-server-handler [this]
    (servlet-context-handler {:servlets this}))
  Handler
  (to-server-handler [this] this)
  nil
  (to-server-handler [_] nil))

(defn ^Server configure-server!
  [^Server server config]
  (if (contains? config :connectors)
    (->> (:connectors config)
         (map (partial server-connector server))
         (into-array ServerConnector)
         (.setConnectors server)))
  (if (contains? config :handler)
    (.setHandler server (to-server-handler (:handler config))))
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

(defn ^Server start
  [^Server server]
  (doto server
    (.start)))

(defn ^Server stop
  [^Server server]
  (doto server
    (.stop)))

(defn ^Server join
  [^Server server]
  (doto server
    (.join)))
