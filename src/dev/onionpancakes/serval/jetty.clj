(ns dev.onionpancakes.serval.jetty
  (:require [dev.onionpancakes.serval.core :as c])
  (:import [jakarta.servlet Servlet]
           [org.eclipse.jetty.server Server Handler
            ConnectionFactory
            ServerConnector HttpConnectionFactory HttpConfiguration]
           [org.eclipse.jetty.servlet
            ServletHolder ServletContextHandler]))

;; Connectors

(defn http-configuration
  [config]
  (let [http-config (HttpConfiguration.)]
    (if-let [ssv? (:send-server-version? config)]
      (.setSendServerVersion http-config ssv?))
    http-config))

(defmulti connection-factories :type)

(defmethod connection-factories :http
  [config]
  (let [http-config (http-configuration config)]
    [(HttpConnectionFactory. http-config)]))

(defn server-connector
  [server config]
  (let [connector (ServerConnector. server)]
    (.setConnectionFactories connector (connection-factories config))
    (if-let [port (:port config)]
      (.setPort connector port))
    (if-let [host (:host config)]
      (.setHost connector host))
    connector))

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

(defn server
  [config]
  (let [server (Server.)]
    (.setHandler server (handler (:handler config)))
    (doseq [conn-config (:connectors config)]
      (.addConnector server (server-connector server conn-config)))
    server))
