(ns dev.onionpancakes.serval.jetty
  (:require [dev.onionpancakes.serval.jetty.impl.server
             :as impl.server]
            [dev.onionpancakes.serval.jetty.impl.ee10.servlet
             :as impl.ee10.servlet]
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

(def as-server-handler
  impl.ee10.servlet/as-servlet-context-handler)

(defn configure-server
  [server config]
  (let [cf (cond-> config
             (contains? config :handler) (update :handler as-server-handler))]
    (impl.server/configure-server server cf)))

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
