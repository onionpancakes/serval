(ns dev.onionpancakes.serval.jetty
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

;; Handler

(defn servlet-context-handler
  [config]
  (let [^ServletContextHandler handler (ServletContextHandler.)]
    (doseq [[^String path ^Servlet servlet] config]
      (.addServlet handler (ServletHolder. servlet) path))
    handler))

;; Server

(defn server
  [config]
  (let [server (Server.)]
    (.setHandler server (servlet-context-handler (:context config)))
    (doseq [conn-config (:connectors config)]
      (.addConnector server (server-connector server conn-config)))
    server))
