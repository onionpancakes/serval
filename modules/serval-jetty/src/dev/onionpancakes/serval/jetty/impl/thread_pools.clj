(ns dev.onionpancakes.serval.jetty.impl.thread-pools
  (:import [org.eclipse.jetty.util.thread QueuedThreadPool]))

;; QueuedThreadPool

(defn queued-thread-pool
  ^QueuedThreadPool
  [config]
  (let [pool (QueuedThreadPool.)]
    (when (contains? config :min-threads)
      (.setMinThreads pool (:min-threads config)))
    (when (contains? config :max-threads)
      (.setMaxThreads pool (:max-threads config)))
    (when (contains? config :idle-timeout)
      (.setIdleTimeout pool (:idle-timeout config)))
    pool))
