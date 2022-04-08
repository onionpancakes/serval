(ns dev.onionpancakes.serval.test-utils.server
  (:require [dev.onionpancakes.serval.core :as srv])
  (:import [org.eclipse.jetty.server
            Server Handler ServerConnector
            HttpConnectionFactory HttpConfiguration]
           [org.eclipse.jetty.servlet ServletHolder ServletContextHandler]))

(def port 42000)

(defn ^:dynamic handler
  [ctx]
  {:serval.response/status 200})

(defonce server-handler
  (doto (ServletContextHandler.)
    (.addServlet (ServletHolder. (srv/http-servlet2 #'handler)) "/*")))

(defonce server
  (doto (Server. ^int port)
    (.setHandler server-handler)))
