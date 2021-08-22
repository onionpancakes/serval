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
  (let [http-config (HttpConfiguration.)]
    (if-let [ssv? (:send-server-version? config)]
      (.setSendServerVersion http-config ssv?))
    http-config))

(defmulti connection-factories :protocol)

(defmethod connection-factories :http
  [config]
  (let [http-config (http-configuration config)
        http1       (HttpConnectionFactory. http-config)]
    [http1]))

(defmethod connection-factories :http2c
  [config]
  (let [http-config (http-configuration config)
        http1       (HttpConnectionFactory. http-config)
        http2       (HTTP2CServerConnectionFactory. http-config)]
    [http1 http2]))

(defn configure-connector!
  [conn config]
  (doto conn
    (.setConnectionFactories (connection-factories config))
    (cond-> (:port config) (.setPort (:port config))
            (:host config) (.setHost (:host config)))))

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
    (servlet-context-handler [["/" this]]))
  clojure.lang.Var
  (handler [this]
    (servlet-context-handler [["/" this]]))
  Servlet
  (handler [this]
    (servlet-context-handler [["/" this]]))
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
