(ns dev.onionpancakes.serval.jetty.impl.protocols)

(defprotocol ServerHandler
  (^org.eclipse.jetty.server.Handler as-server-handler [this]))

(defprotocol ContextHandler
  (^org.eclipse.jetty.server.handler.ContextHandler as-context-handler [this]))

(defprotocol GzipHandler
  (as-gzip-handler [this]))
