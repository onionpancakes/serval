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
         n)))

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
  [data ^ServletRequest req ^java.io.InputStream out]
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

(defrecord MockReadListener [^ServletInputStream parent
                             ^java.io.OutputStream output-stream
                             cf]
  ReadListener
  (onDataAvailable [this]
    (loop []
      (let [buffer (byte-array 1024)
            nread  (.read parent buffer)]
        (when-not (== nread -1)
          (.write output-stream buffer 0 nread)
          (if (.isReady parent) (recur))))))
  (onAllDataRead [this]
    (.complete cf))
  (onError [this throwable]
    (.completeExceptionally cf throwable)))

(defrecord MockWriteListener [^ServletOutputStream parent
                              ^java.io.InputStream input-stream
                              cf]
  WriteListener
  (onWritePossible [this]
    (loop []
      (let [byte-read (.read input-stream)]
        (when-not (== byte-read -1)
          (.write parent byte-read)
          (if (.isReady parent) (recur))))))
  (onError [this throwable]))

(defn read-listener
  [parent output-stream cf]
  (MockReadListener. parent output-stream cf))

(defn write-listener
  [parent input-stream cf]
  (MockWriteListener. parent input-stream cf))

(defn read-async!
  [in out]
  (let [cf (CompletableFuture.)
        rl (read-listener in out cf)]
    (.setReadListener in rl)
    cf))

(defn write-async!
  [out in]
  (let [cf (CompletableFuture.)
        wl (write-listener out in cf)]
    (.setWriteListener out wl)
    cf))


