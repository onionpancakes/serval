(ns dev.onionpancakes.serval.jetty.impl.protocols
  (:import [org.eclipse.jetty.server Handler]))

(defprotocol ServerHandler
  (as-server-handler ^Handler [this]))

(defprotocol GzipHandler
  (as-gzip-handler [this]))
