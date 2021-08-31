(ns dev.onionpancakes.serval.io
  (:import [java.util.concurrent CompletableFuture]
           [jakarta.servlet ServletResponse ServletOutputStream WriteListener]))

;; Body

(defprotocol ResponseBody
  (async-body? [this])
  (write-body [this resp] "Write body to ServletResponse. Return CompletionStage for async write. Otherwise, return nil for sync write."))

(extend-protocol ResponseBody
  (Class/forName "[B")
  (async-body? [this] false)
  (write-body [this ^ServletResponse resp]
    (.write (.getOutputStream resp) ^bytes this))
  String
  (async-body? [this] false)
  (write-body [this ^ServletResponse resp]
    (.write (.getWriter resp) this))
  java.io.InputStream
  (async-body? [this] false)
  (write-body [this ^ServletResponse resp]
    (try
      (.transferTo this (.getOutputStream resp))
      (finally
        (.close this))))
  nil
  (async-body? [this] false)
  (write-body [this resp] nil))

;; Async write support

(deftype BytesWriteListener [^bytes bytes
                             ^:unsynchronized-mutable ^int offset
                             ^long length
                             ^ServletOutputStream os
                             ^CompletableFuture cf]
  WriteListener
  (onWritePossible [this]
    (loop []
      (if (< offset length)
        (do
          (.write os (aget bytes offset))
          (set! offset (unchecked-inc-int offset))
          (if (.isReady os) (recur)))
        (.complete cf nil))))
  (onError [this throwable]
    (.completeExceptionally cf throwable)))

(defn bytes-write-listener
  [^bytes bytes os cf]
  (BytesWriteListener. bytes 0 (alength bytes) os cf))

(defrecord AsyncBytes [bytes]
  ResponseBody
  (async-body? [this] true)
  (write-body [this resp]
    (let [os (.getOutputStream ^ServletResponse resp)
          cf (CompletableFuture.)
          cb (bytes-write-listener bytes os cf)]
      (.setWriteListener os cb)
      ;; Return CompletionStage to signal async body.
      cf)))

(defn async-bytes
  [bytes]
  (AsyncBytes. bytes))
