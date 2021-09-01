(ns dev.onionpancakes.serval.io.body
  (:import [java.util.concurrent CompletableFuture]
           [java.nio ByteBuffer]
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

(deftype BytesReadChunk [bytes ^int length])

(defn chunks-total-length
  [^java.lang.Iterable chunks]
  (let [iter (.iterator chunks)]
    (loop [total-length 0]
      (if (.hasNext iter)
        (recur (->> (.-length ^BytesReadChunk (.next iter))
                    (unchecked-add total-length)))
        total-length))))

(defn chunks-concat-bytes
  [^java.lang.Iterable chunks]
  (let [size   (chunks-total-length chunks)
        barray (byte-array size)
        buffer (ByteBuffer/wrap barray)
        iter   (.iterator chunks)]
    (while (.hasNext iter)
      (let [^BytesReadChunk chunk (.next iter)]
        (.put buffer (.-bytes chunk) 0 (.-length chunk))))
    barray))

(deftype BytesReadListener [chunk-size
                            ^java.util.List chunks
                            ^ServletInputStream is
                            ^CompletableFuture cf]
  ReadListener
  (onAllDataRead [this]
    (.complete cf (chunks-concat-bytes chunks)))
  (onDataAvailable [this]
    (loop []
      ;; Note: chunk-size is boxed. (byte-array) can't take primitives?
      (let [buffer (byte-array chunk-size)
            nread  (.read is buffer)]
        (when-not (== nread -1)
          (.add chunks (BytesReadChunk. buffer nread))
          (if (.isReady is) (recur))))))
  (onError [this throwable]
    (.completeExceptionally cf throwable)))

(defn bytes-read-listener
  [chunk-size is cf]
  (BytesReadListener. chunk-size (java.util.ArrayList.) is cf))

(defn read-body-as-bytes-async!
  [^ServletRequest request]
  (let [cf (CompletableFuture.)
        is (.getInputStream request)
        rl (bytes-read-listener 1024 is cf)]
    (.startAsync request)
    (.setReadListener is rl)
    cf))

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
