(ns dev.onionpancakes.serval.io.body
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function]
           [java.nio ByteBuffer]
           [java.nio.charset Charset]
           [jakarta.servlet
            ServletRequest ServletResponse
            ServletInputStream ServletOutputStream
            ReadListener WriteListener]))

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

;; Async read support

(defn- into-buffer!
  [^ByteBuffer to ^java.lang.Iterable from]
  (let [iter (.iterator from)]
    (while (.hasNext iter)
      (let [^ByteBuffer buf (.next iter)]
        (.put to buf))))
  to)

(deftype BufferReadListener [^long chunk-capacity
                             ^java.util.List chunks
                             ^ServletInputStream in
                             ^CompletableFuture cf]
  ReadListener
  (onAllDataRead [this]
    (let [capacity (unchecked-multiply-int chunk-capacity (.size chunks))
          buffer   (ByteBuffer/allocate capacity)
          _        (into-buffer! buffer chunks)
          _        (.flip buffer)]
      (.complete cf buffer)))
  (onDataAvailable [this]
    (loop []
      (let [chunk (ByteBuffer/allocate chunk-capacity)
            nread (.read in (.array chunk))]
        (when-not (== nread -1)
          (.limit chunk nread)
          (.add chunks chunk)
          (if (.isReady in) (recur))))))
  (onError [this throwable]
    (.completeExceptionally cf throwable)))

(defn buffer-read-listener
  [chunk-capacity in cf]
  (BufferReadListener. chunk-capacity (java.util.ArrayList.) in cf))

(defn ^CompletableFuture read-body-as-buffer-async!
  [^ServletRequest request]
  (let [cf (CompletableFuture.)
        in (.getInputStream request)
        rl (buffer-read-listener 1024 in cf)]
    (.startAsync request)
    (.setReadListener in rl)
    cf))

(defn  ^CompletableFuture read-body-as-string-async!
  [^ServletRequest request]
  (let [charset (if-let [enc (.getCharacterEncoding request)]
                  (Charset/forName enc)
                  (Charset/defaultCharset))]
    (-> (read-body-as-buffer-async! request)
        (.thenApply (reify Function
                      (apply [_ buf]
                        (.toString (.decode charset buf))))))))

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

(defprotocol AsyncWritable
  (write-listener [this ctx cf]))

(extend-protocol AsyncWritable

  (Class/forName "[B")
  (write-listener [this {^ServletResponse resp :serval.service/response} cf]
    (bytes-write-listener this (.getOutputStream resp) cf))

  String
  (write-listener [this {^ServletResponse resp :serval.service/response} cf]
    (-> (.getBytes this (.getCharacterEncoding resp))
        (bytes-write-listener (.getOutputStream resp) cf))))

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
            wl (write-listener body ctx cf)]
        (.. resp getOutputStream (setWriteListener wl))
        cf)
      ;; Sync write if async not supported.
      (write-body body ctx))))

(defn async-body
  [body]
  (AsyncBody. body))
