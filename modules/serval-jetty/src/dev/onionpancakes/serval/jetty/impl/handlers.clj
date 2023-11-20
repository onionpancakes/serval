(ns dev.onionpancakes.serval.jetty.impl.handlers
  (:refer-clojure :exclude [error-handler])
  (:require [dev.onionpancakes.serval.impl.http.servlet
             :as srv.impl.http.servlet]
            [dev.onionpancakes.serval.jetty.impl.protocols :as p])
  (:import [org.eclipse.jetty.server.handler ContextHandler ContextHandlerCollection]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]))

;; ContextHandlerCollection

(defn context-handler-collection
  [config]
  (let [dynamic    (:dynamic config false)
        contexts   (into-array ContextHandler [])
        collection (ContextHandlerCollection. dynamic contexts)]
    (when (contains? config :handlers)
      (.setHandlers collection (:handlers config)))
    collection))

;; GzipHandler

(defn gzip-handler
  [config]
  (let [handler (GzipHandler.)]
    (when (contains? config :excluded-methods)
      (->> (:excluded-methods config)
           (map name)
           (into-array String)
           (.setExcludedMethods handler)))
    (when (contains? config :excluded-mime-types)
      (->> (:excluded-mime-types config)
           (into-array String)
           (.setExcludedMimeTypes handler)))
    (when (contains? config :excluded-paths)
      (->> (:excluded-paths config)
           (into-array String)
           (.setExcludedPaths handler)))
    (when (contains? config :included-methods)
      (->> (:included-methods config)
           (map name)
           (into-array String)
           (.setIncludedMethods handler)))
    (when (contains? config :included-mime-types)
      (->> (:included-mime-types config)
           (into-array String)
           (.setIncludedMimeTypes handler)))
    (when (contains? config :included-paths)
      (->> (:included-paths config)
           (into-array String)
           (.setIncludedPaths handler)))
    (when (contains? config :min-gzip-size)
      (.setMinGzipSize handler (:min-gzip-size config)))
    handler))

(extend-protocol p/GzipHandler
  clojure.lang.IPersistentMap
  (as-gzip-handler [this]
    (gzip-handler this))
  GzipHandler
  (as-gzip-handler [this] this))

(defn as-gzip-handler
  [this]
  (p/as-gzip-handler this))
