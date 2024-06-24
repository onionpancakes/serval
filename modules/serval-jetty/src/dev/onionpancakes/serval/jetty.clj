(ns dev.onionpancakes.serval.jetty
  (:require [dev.onionpancakes.serval.jetty.impl.handlers
             :as impl.handlers]
            [dev.onionpancakes.serval.jetty.impl.server
             :as impl.server]
            [dev.onionpancakes.serval.jetty.impl.thread-pools
             :as impl.thread-pools])
  (:import [org.eclipse.jetty.server Server CustomRequestLog]
           [org.eclipse.jetty.util.thread QueuedThreadPool]))

;; Thread pool

(defn queued-thread-pool
  {:tag QueuedThreadPool}
  ([]
   (impl.thread-pools/queued-thread-pool))
  ([config]
   (impl.thread-pools/queued-thread-pool config)))

;; CustomRequestLog

(defn custom-request-log
  []
  (CustomRequestLog.))

;; Handlers

(defn gzip-handler
  [& {:as opts}]
  (impl.handlers/gzip-handler opts))

;; Server

(defn configure
  [server config]
  (impl.server/configure server config))

(defn server
  {:tag Server}
  ([]
   (impl.server/server))
  ([pool]
   (impl.server/server pool)))

(defn start
  {:tag Server}
  [^Server server]
  (doto server
    (.start)))

(defn stop
  {:tag Server}
  [^Server server]
  (doto server
    (.stop)))

(defn join
  {:tag Server}
  [^Server server]
  (doto server
    (.join)))

(defn restart
  {:tag Server}
  ([^Server server]
   (restart server nil))
  ([^Server server config]
   (doto server
     (stop)
     (configure config)
     (start))))
