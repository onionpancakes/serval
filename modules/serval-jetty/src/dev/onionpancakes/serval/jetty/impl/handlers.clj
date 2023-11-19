(ns dev.onionpancakes.serval.jetty.impl.handlers
  (:refer-clojure :exclude [error-handler])
  (:require [dev.onionpancakes.serval.impl.http.servlet
             :as srv.impl.http.servlet]
            [dev.onionpancakes.serval.jetty.impl.protocols :as p])
  (:import [org.eclipse.jetty.server.handler.gzip GzipHandler]))

;; GzipHandler

(defn gzip-handler
  [config]
  (let [gzhandler (GzipHandler.)]
    (if (contains? config :excluded-methods)
      (->> (:excluded-methods config)
           (map name)
           (into-array String)
           (.setExcludedMethods gzhandler)))
    (if (contains? config :excluded-mime-types)
      (->> (:excluded-mime-types config)
           (into-array String)
           (.setExcludedMimeTypes gzhandler)))
    (if (contains? config :excluded-paths)
      (->> (:excluded-paths config)
           (into-array String)
           (.setExcludedPaths gzhandler)))
    (if (contains? config :included-methods)
      (->> (:included-methods config)
           (map name)
           (into-array String)
           (.setIncludedMethods gzhandler)))
    (if (contains? config :included-mime-types)
      (->> (:included-mime-types config)
           (into-array String)
           (.setIncludedMimeTypes gzhandler)))
    (if (contains? config :included-paths)
      (->> (:included-paths config)
           (into-array String)
           (.setIncludedPaths gzhandler)))
    (if (contains? config :min-gzip-size)
      (.setMinGzipSize gzhandler (:min-gzip-size config)))
    gzhandler))

(extend-protocol p/GzipHandler
  clojure.lang.IPersistentMap
  (as-gzip-handler [this]
    (gzip-handler this))
  Boolean
  (as-gzip-handler [this]
    (if this (gzip-handler nil) nil))
  GzipHandler
  (as-gzip-handler [this] this)
  nil
  (as-gzip-handler [_] nil))

(defn as-gzip-handler
  [this]
  (p/as-gzip-handler this))
