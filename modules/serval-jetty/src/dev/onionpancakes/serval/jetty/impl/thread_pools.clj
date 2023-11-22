(ns dev.onionpancakes.serval.jetty.impl.thread-pools
  (:import [org.eclipse.jetty.util.thread QueuedThreadPool]
           [org.eclipse.jetty.util VirtualThreads]
           [java.util.concurrent Executor]))

;; QueuedThreadPool

(defn queued-thread-pool
  ([] (queued-thread-pool nil))
  ([config]
   (let [pool (QueuedThreadPool.)]
     (when (contains? config :min-threads)
       (.setMinThreads pool (:min-threads config)))
     (when (contains? config :max-threads)
       (.setMaxThreads pool (:max-threads config)))
     (when (contains? config :idle-timeout)
       (.setIdleTimeout pool (:idle-timeout config)))
     (when (contains? config :virtual-threads)
       (let [vt (:virtual-threads config)]
         (cond
           (true? vt)              (->> (VirtualThreads/getDefaultVirtualThreadsExecutor)
                                        (.setVirtualThreadsExecutor pool))
           (instance? Executor vt) (.setVirtualThreadsExecutor pool vt)
           (false? vt)             (.setVirtualThreadsExecutor pool nil)
           (nil? vt)               (.setVirtualThreadsExecutor pool nil))))
     pool)))
