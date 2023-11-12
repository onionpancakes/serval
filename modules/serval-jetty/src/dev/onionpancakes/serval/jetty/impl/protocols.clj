(ns dev.onionpancakes.serval.jetty.impl.protocols
  (:require [dev.onionpancakes.serval.servlet :as srv.servlet]))

;; Core

(defprotocol Servlet
  (as-servlet [this]))

(extend-protocol Servlet
  clojure.lang.AFunction
  (as-servlet [this]
    (srv.servlet/servlet this))
  clojure.lang.Var
  (as-servlet [this]
    (srv.servlet/servlet this))
  jakarta.servlet.Servlet
  (as-servlet [this] this))

(defprotocol ServletContextPaths
  (as-servlet-context-paths [this]))

(extend-protocol ServletContextPaths
  clojure.lang.PersistentVector
  (as-servlet-context-paths [this] this)
  Object
  (as-servlet-context-paths [this] [["/*" this]]))

;; Server

(defprotocol ErrorHandler
  (as-error-handler [this]))

(defprotocol GzipHandler
  (as-gzip-handler [this]))
