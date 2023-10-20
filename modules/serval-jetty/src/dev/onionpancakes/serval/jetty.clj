(ns dev.onionpancakes.serval.jetty
  (:require [dev.onionpancakes.serval.jetty.impl.server
             :as impl.server]
            [dev.onionpancakes.serval.jetty.impl.thread-pools
             :as impl.thread-pools])
  (:import [org.eclipse.jetty.server Server CustomRequestLog]
           [org.eclipse.jetty.util.thread QueuedThreadPool]))

;; Thread pool

(defn queued-thread-pool
  ^QueuedThreadPool
  [config]
  (impl.thread-pools/queued-thread-pool config))

;; Server

(def as-server-handler-fn
  (delay
    (require '[dev.onionpancakes.serval.jetty.impl.ee10.servlet])
    (resolve 'dev.onionpancakes.serval.jetty.impl.ee10.servlet/as-servlet-context-handler)))

(defn server-config
  [config]
  (cond-> config
    (contains? config :handler) (update :handler @as-server-handler-fn)))

(defn configure-server
  [server config]
  (impl.server/configure-server server (server-config config)))

(def default-server-config
  {:request-log (CustomRequestLog.)})

(defn server
  (^Server [config]
   (doto (impl.server/server)
     (configure-server (merge default-server-config config))))
  (^Server [pool config]
   (doto (impl.server/server pool)
     (configure-server (merge default-server-config config)))))

(defn start
  ^Server
  [^Server server]
  (doto server
    (.start)))

(defn stop
  ^Server
  [^Server server]
  (doto server
    (.stop)))

(defn join
  ^Server
  [^Server server]
  (doto server
    (.join)))

(defn restart
  (^Server [^Server server]
   (restart server nil))
  (^Server [^Server server config]
   (doto server
     (stop)
     (configure-server config)
     (start))))
