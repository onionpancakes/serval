(ns dev.onionpancakes.serval.jetty
  (:require [dev.onionpancakes.serval.core :as c])
  (:import [jakarta.servlet Servlet]
           [org.eclipse.jetty.server
            Server Handler
            ServerConnector ConnectionFactory
            HttpConnectionFactory HttpConfiguration]
           [org.eclipse.jetty.http2.server HTTP2CServerConnectionFactory]
           [org.eclipse.jetty.servlet ServletHolder ServletContextHandler]))

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
  [conn config]
  (.setConnectionFactories conn (connection-factories config))
  (if (contains? config :port)
    (.setPort conn (:port config)))
  (if (contains? config :host)
    (.setHost conn (:host config)))
  (if (contains? config :idle-timeout)
    (.setIdleTimeout conn (:idle-timeout config))))

;; Servlet

(defprotocol IServlet
  (^Servlet servlet [this]))

(extend-protocol IServlet
  clojure.lang.Fn
  (servlet [this] (c/servlet this))
  clojure.lang.Var
  (servlet [this] (c/servlet this))
  Servlet
  (servlet [this] this))

;; Handler

(defn servlet-context-handler
  [path-to-servlets]
  (let [^ServletContextHandler sch (ServletContextHandler.)]
    (doseq [[^String path serv] path-to-servlets]
      (.addServlet sch (ServletHolder. (servlet serv)) path))
    sch))

(defprotocol IHandler
  (handler [this]))

(extend-protocol IHandler
  clojure.lang.Fn
  (handler [this]
    (servlet-context-handler [["/*" this]]))
  clojure.lang.Var
  (handler [this]
    (servlet-context-handler [["/*" this]]))
  Servlet
  (handler [this]
    (servlet-context-handler [["/*" this]]))
  java.util.Collection
  (handler [this]
    (servlet-context-handler this))
  Handler
  (handler [this] this))

;; Server

(defn configure-server!
  [server config]
  (.setHandler server (handler (:handler config)))
  (doseq [conn-config (:connectors config)
          :let        [conn (ServerConnector. server)]]
    (configure-connector! conn conn-config)
    (.addConnector server conn)))

(defn server
  [config]
  (doto (Server.)
    (configure-server! config)))
