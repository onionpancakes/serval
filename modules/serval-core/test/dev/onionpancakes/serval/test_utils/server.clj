(ns dev.onionpancakes.serval.test-utils.server
  (:require [dev.onionpancakes.serval.core :as srv])
  (:import [jakarta.servlet Servlet]
           [org.eclipse.jetty.server Server]
           [org.eclipse.jetty.servlet ServletHolder ServletContextHandler]))

(defn set-handler!
  [^Server server ^Servlet servlet]
  (let [holder  (ServletHolder. servlet)
        handler (ServletContextHandler.)
        _       (.addServlet handler holder "/*")]
    (doto server
      (.setHandler handler))))

(defn reset-handler!
  [^Server server]
  (doto server
    (.setHandler nil)))

;; Test server

(def port 42000)

(defonce ^Servlet server
  (Server. ^int port))

(defmacro with-handler
  [f & body]
  `(try
     (.stop server)
     (set-handler! server (srv/http-servlet ~f))
     (.start server)
     ~@body
     (finally
       (.stop server)
       (.join server)
       (reset-handler! server))))

(defmacro with-response
  [resp & body]
  `(with-handler (constantly ~resp) ~@body))
