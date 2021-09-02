(ns dev.onionpancakes.serval.io.body
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function]
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
  ([request] (read-body-as-bytes-async! request nil))
  ([^ServletRequest request {:keys [chunk-size]
                             :or   {chunk-size 1024}}]
   (let [cf (CompletableFuture.)
         is (.getInputStream request)
         rl (bytes-read-listener chunk-size is cf)]
     (.startAsync request)
     (.setReadListener is rl)
     cf)))

(defn read-body-as-string-async!
  ([request] (read-body-as-string-async! request nil))
  ([^ServletRequest request opts]
   (-> ^CompletionStage (read-body-as-bytes-async! request opts)
       (.thenApply (reify Function
                     (apply [_ input]
                       (String. ^bytes input (.getCharacterEncoding request))))))))

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
