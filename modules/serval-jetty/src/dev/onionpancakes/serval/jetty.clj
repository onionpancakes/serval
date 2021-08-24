(ns dev.onionpancakes.serval.jetty
  (:import [jakarta.servlet Servlet]
           [org.eclipse.jetty.server
            Server Handler ServerConnector
            ConnectionFactory HttpConnectionFactory HttpConfiguration]
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
  [^ServerConnector conn config]
  (.setConnectionFactories conn (connection-factories config))
  (if (contains? config :port)
    (.setPort conn (:port config)))
  (if (contains? config :host)
    (.setHost conn (:host config)))
  (if (contains? config :idle-timeout)
    (.setIdleTimeout conn (:idle-timeout config))))

;; Handler

(defn servlet-context-handler
  [path-to-servlets]
  (let [^ServletContextHandler sch (ServletContextHandler.)]
    (doseq [[^String path ^Servlet serv] path-to-servlets]
      (.addServlet sch (ServletHolder. serv) path))
    sch))

(defn handler
  [config]
  (cond
    (:servlets config) (servlet-context-handler (:servlets config))
    (:servlet config)  (servlet-context-handler [["/*" (:servlet config)]])
    :else              nil))

;; Server

(defn configure-server!
  [^Server server config]
  (if-let [h (handler config)]
    (.setHandler server h))
  (->> (:connectors config)
       (map #(doto (ServerConnector. server)
               (configure-connector! %)))
       (into-array ServerConnector)
       (.setConnectors server)))

(defn server
  [config]
  (doto (Server.)
    (configure-server! config)))
