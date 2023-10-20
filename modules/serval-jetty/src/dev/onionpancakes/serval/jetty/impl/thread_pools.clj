(ns dev.onionpancakes.serval.jetty.impl.thread-pools
  (:import [org.eclipse.jetty.util.thread QueuedThreadPool]))

;; QueuedThreadPool

(defn queued-thread-pool
  ^QueuedThreadPool
  [config]
  (let [pool (QueuedThreadPool.)]
    (if (contains? config :min-threads)
      (.setMinThreads pool (:min-threads config)))
    (if (contains? config :max-threads)
      (.setMaxThreads pool (:max-threads config)))
    (if (contains? config :idle-timeout)
      (.setIdleTimeout pool (:idle-timeout config)))
    pool))