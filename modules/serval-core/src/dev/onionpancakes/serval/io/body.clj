(ns dev.onionpancakes.serval.io.body
  (:import [java.util.concurrent CompletableFuture]
           [jakarta.servlet
            ServletRequest ServletResponse
            ServletOutputStream WriteListener]))

;; Body

(defprotocol ResponseBody
  (async-body? [this ctx])
  (write-body [this ctx] "Write body to ServletResponse. Return CompletionStage for async write. Otherwise, return nil for sync write."))

(extend-protocol ResponseBody
  
  (Class/forName "[B")
  (async-body? [this _] false)
  (write-body [this {^ServletResponse resp :serval.service/response}]
    (.. resp getOutputStream (write ^bytes this)))
  
  String
  (async-body? [this _] false)
  (write-body [this {^ServletResponse resp :serval.service/response}]
    (.. resp getWriter (write this)))
  
  java.io.InputStream
  (async-body? [this _] false)
  (write-body [this {^ServletResponse resp :serval.service/response}]
    (try
      (.transferTo this (.getOutputStream resp))
      (finally
        (.close this))))
  
  nil
  (async-body? [this _] false)
  (write-body [this _] nil))

;; Async write support

(deftype BytesWriteListener [^bytes bytes
                             ^:unsynchronized-mutable ^int offset
                             ^long length
                             ^ServletOutputStream os
                             cb]
  WriteListener
  (onWritePossible [this]
    (loop []
      (if (< offset length)
        (do
          (.write os (aget bytes offset))
          (set! offset (unchecked-inc-int offset))
          (if (.isReady os) (recur)))
        (cb))))
  (onError [this throwable]
    (cb throwable)))

(defn bytes-write-listener
  [^bytes bytes os cb]
  (BytesWriteListener. bytes 0 (alength bytes) os cb))

(defn set-bytes-write-listener!
  [bytes ^ServletResponse resp cb]
  (let [os (.getOutputStream resp)
        wl (bytes-write-listener bytes os cb)]
    (.setWriteListener os wl)))

(defrecord AsyncBytes [bytes]
  ResponseBody
  (async-body? [this {^ServletRequest req :serval.service/request}]
    (.isAsyncSupported req))
  (write-body [this {^ServletRequest req   :serval.service/request
                     ^ServletResponse resp :serval.service/response}]
    (if (.isAsyncSupported req)
      ;; Async write if supported.
      (let [cf (CompletableFuture.)
            cb (fn
                 ([] (.complete cf nil))
                 ([err] (.completeExceptionally cf err)))]
        (set-bytes-write-listener! bytes resp cb)
        cf)
      ;; Sync write when async not supported.
      (.. resp getOutputStream (write ^bytes bytes)))))

(defn async-bytes
  [bytes]
  (AsyncBytes. bytes))
