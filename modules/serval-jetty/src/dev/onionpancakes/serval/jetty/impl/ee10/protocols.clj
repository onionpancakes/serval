(ns dev.onionpancakes.serval.jetty.impl.ee10.protocols)

(defprotocol ServletHolder
  (as-servlet-holder [this]))

(defprotocol ServletContextHandler
  (as-servlet-context-handler [this]))
