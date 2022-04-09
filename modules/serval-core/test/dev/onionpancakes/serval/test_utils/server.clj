(ns dev.onionpancakes.serval.test-utils.server
  (:require [dev.onionpancakes.serval.core :as srv])
  (:import [org.eclipse.jetty.server
            Server Handler ServerConnector
            HttpConnectionFactory HttpConfiguration]
           [org.eclipse.jetty.servlet ServletHolder ServletContextHandler]))

(defn set-handler!
  [server servlet]
  (let [holder  (ServletHolder. servlet)
        handler (ServletContextHandler.)
        _       (.addServlet handler holder "/*")]
    (doto server
      (.setHandler handler))))

(defn reset-handler!
  [server]
  (doto server
    (.setHandler nil)))

;; Test server

(def port 42000)

(defonce server
  (Server. ^int port))

(defmacro with-handler
  [f & body]
  `(do
     (.stop server)
     (set-handler! server (srv/http-servlet2 ~f))
     (.start server)
     ~@body
     (.stop server)
     (.join server)
     (reset-handler! server)))

(defmacro with-response
  [resp & body]
  `(with-handler (constantly ~resp) ~@body))
