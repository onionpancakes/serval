(ns dev.onionpancakes.serval.mock.io
  (:import [jakarta.servlet
            ServletInputStream ServletOutputStream
            ReadListener WriteListener
            ServletRequest]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.util.concurrent CompletableFuture]))

(defn servlet-input-stream
  [data ^ServletRequest req ^java.io.InputStream in]
  (proxy [ServletInputStream] []
    (read
      ([]
       (let [n (.read in)]
         (if (== n -1)
           (swap! data assoc :finished? true))
         n))
      ([bytes]
       (let [n (.read in bytes)]
         (if (== n -1)
           (swap! data assoc :finished? true))
         n))
      ([b off len] (.read in b off len)))

    ;; Synchronous impl.
    (isReady [] true)
    (isFinished [] (:finished? @data))
    (setReadListener [^ReadListener cb]
      (try
        ;; Operation must be in async mode.
        ;; This will throw if not.
        (.getAsyncContext req)
        (.onDataAvailable cb)
        (if (:finished? @data)
          (.onAllDataRead cb))
        (catch Exception e
          (.onError cb e))))))

(defn servlet-output-stream
  [data ^ServletRequest req ^java.io.OutputStream out]
  (proxy [ServletOutputStream] []
    (write
      ([b]
       (if (bytes? b)
         (.write out ^bytes b)
         (.write out ^int b)))
      ([b off len]
       (.write out b off len)))

    ;; Synchronous impl.
    (isReady [] true)
    (setWriteListener [^WriteListener cb]
      (try
        ;; Operation must be in async mode.
        ;; This will throw if not.
        (.getAsyncContext req)
        (.onWritePossible cb)
        (catch Exception e
          (.onError cb e))))))

(defn read-listener
  [^ServletInputStream in ^java.io.OutputStream out ^CompletableFuture cf]
  (reify ReadListener
    (onDataAvailable [this]
      (loop []
        (let [buffer (byte-array 1024)
              nread  (.read in buffer)]
          (when-not (== nread -1)
            (.write out buffer 0 nread)
            (if (.isReady in) (recur))))))
    (onAllDataRead [this]
      (.complete cf nil))
    (onError [this throwable]
      (.completeExceptionally cf throwable))))

(defn write-listener
  [^ServletOutputStream out ^java.io.InputStream in ^CompletableFuture cf]
  (reify WriteListener
    (onWritePossible [this]
      (loop []
        (let [byte-read (.read in)]
          (if-not (== byte-read -1)
            (do
              (.write out byte-read)
              (if (.isReady out) (recur)))
            (.complete cf nil)))))
    (onError [this throwable])))

(defn read-async!
  [^ServletInputStream in out]
  (let [cf (CompletableFuture.)
        rl (read-listener in out cf)]
    (.setReadListener in rl)
    cf))

(defn write-async!
  [^ServletOutputStream out in]
  (let [cf (CompletableFuture.)
        wl (write-listener out in cf)]
    (.setWriteListener out wl)
    cf))


