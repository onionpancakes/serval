(ns dev.onionpancakes.serval.jetty.impl.handlers
  (:import [org.eclipse.jetty.server.handler ContextHandler ContextHandlerCollection]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]))

;; ContextHandlerCollection

(defn context-handler-collection
  [config]
  (let [dynamic    (:dynamic config false)
        contexts   (into-array ContextHandler [])
        collection (ContextHandlerCollection. dynamic contexts)]
    (when (contains? config :handlers)
      (.setHandlers collection ^java.util.List (:handlers config)))
    collection))

;; GzipHandler

(defn gzip-handler
  {:tag GzipHandler}
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
