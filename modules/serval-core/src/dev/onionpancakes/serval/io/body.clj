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
      nil ;; Return a nil, because transferTo returns a long.
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

(defprotocol AsyncWritable
  (write-listener [this ctx cb]))

(extend-protocol AsyncWritable

  (Class/forName "[B")
  (write-listener [this {^ServletResponse resp :serval.service/response} cb]
    (bytes-write-listener this (.getOutputStream resp) cb))

  String
  (write-listener [this {^ServletResponse resp :serval.service/response} cb]
    (-> (.getBytes this (.getCharacterEncoding resp))
        (bytes-write-listener (.getOutputStream resp) cb))))

(defrecord AsyncBody [body]
  ResponseBody
  (async-body? [this {^ServletRequest req :serval.service/request :as ctx}]
    (or (.isAsyncSupported req)
        (async-body? body ctx)))
  (write-body [this {^ServletRequest req   :serval.service/request
                     ^ServletResponse resp :serval.service/response
                     :as                   ctx}]
    (if (.isAsyncSupported req)
      ;; Async write if supported.
      ;; Note: will fail if underlying body does not implement AsyncWritable.
      ;; It is possible to test if underlying satisfies AsyncWritable,
      ;; and fall back to sync writes, but performance may suffer.
      (let [cf (CompletableFuture.)
            cb (fn
                 ([] (.complete cf nil))
                 ([err] (.completeExceptionally cf err)))
            wl (write-listener body ctx cb)]
        (.. resp getOutputStream (setWriteListener wl))
        cf)
      ;; Sync write if async not supported.
      (write-body body ctx))))

(defn async-body
  [body]
  (AsyncBody. body))
