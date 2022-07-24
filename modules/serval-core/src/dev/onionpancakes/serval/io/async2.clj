(ns dev.onionpancakes.serval.io.async2
  (:require [dev.onionpancakes.serval.io.body :as io.body])
  (:import [jakarta.servlet
            ServletRequest
            ServletResponse ServletOutputStream WriteListener]
           [java.nio ByteBuffer]
           [java.nio.channels
            AsynchronousByteChannel
            AsynchronousFileChannel
            CompletionHandler]
           [java.nio.file
            StandardOpenOption]
           [java.util.concurrent CompletableFuture]))

(defn write-buffer!
  "Write bytes from ByteBuffer to ServletOutputStream until
  either ByteBuffer is depleted or outputstream is unable to
  to accept more data. Returns true if outputstream remains
  ready or false if not."
  [^ByteBuffer buf ^ServletOutputStream out]
  (loop []
    (if (.hasRemaining buf)
      (do
        (.write out (.get buf))
        (if (.isReady out) (recur) false))
      true)))

(defprotocol AsyncWritable
  (write! [this out] "Write bytes from this to ServetOutputStream.
                      Returns true if outputstream remains ready or
                      false if not."))

(extend-protocol AsyncWritable
  ByteBuffer
  (write! [this out]
    (write-buffer! this out)))

(defprotocol AsyncWritableValue
  (writable [this] "Returns a AsyncWritable from value."))

(extend-protocol AsyncWritableValue
  (Class/forName "[B")
  (writable [this]
    (ByteBuffer/wrap this))
  String
  (writable [this]
    (ByteBuffer/wrap (.getBytes this))))

;; Value write listener

(deftype AsyncWritableWriteListener [data
                                     ^ServletOutputStream out
                                     ^CompletableFuture cf]
  WriteListener
  (onWritePossible [this]
    (if (write! data out)
      (.complete cf nil)))
  (onError [_ throwable]
    (.completeExceptionally cf throwable)))

(defn async-writable-write-listener
  [value out cf]
  (-> (writable value)
      (AsyncWritableWriteListener. out cf)))

;; File write listener

(deftype AsyncFileChannelWriteListener [^AsynchronousFileChannel ch
                                        ^ByteBuffer buf
                                        ^:volatile-mutable ^long pos
                                        ^ServletOutputStream out
                                        ^CompletableFuture cf]
  ;; Impl both WriteListener and CompletionHandler
  ;; on the same type, so both can have access to a private
  ;; volatile-mutable unboxed long pos variable.
  ;;
  ;; Volatile mutable, not unsynchronized mutable, due to
  ;; AsynchronousFileChannel may run its callback on different
  ;; threads.
  WriteListener
  (onWritePossible [this]
    (when (write-buffer! buf out)
      (.clear buf)
      (.read ch buf pos nil this)))
  (onError [_ throwable]
    (.completeExceptionally cf throwable))
  CompletionHandler
  (completed [this n _]
    (if (== (.longValue ^Integer n) -1)
      (.complete cf nil)
      (do
        (.flip buf)
        (set! pos (+ pos (.longValue ^Integer n)))
        (when (write-buffer! buf out)
          (.clear buf)
          (.read ch buf pos nil this)))))
  (failed [_ throwable _]
    (.completeExceptionally cf throwable)))

(def default-buffer-size 4092)

(def default-file-channel-opts
  (->> [StandardOpenOption/READ]
       (into-array StandardOpenOption)))

(defn async-file-channel-write-listener
  ([path out cf]
   (async-file-channel-write-listener path out cf default-buffer-size))
  ([path out cf buffer-size]
   (let [buf (.limit (ByteBuffer/allocate buffer-size) 0)]
     (-> (AsynchronousFileChannel/open path default-file-channel-opts)
         (AsyncFileChannelWriteListener. buf 0 out cf)))))

;; Async Body

(defprotocol ResponseBodyAsync
  (service-body-async [this servlet request response]))

(extend-protocol ResponseBodyAsync
  java.nio.file.Path
  (service-body-async [this _ ^ServletRequest request ^ServletResponse response]
    (let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          wl  (async-file-channel-write-listener this out cf)
          _   (if-not (.isAsyncStarted request)
                (.startAsync request))
          _   (.setWriteListener out wl)]
      cf))
  java.io.File
  (service-body-async [this _ ^ServletRequest request ^ServletResponse response]
    (let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          wl  (async-file-channel-write-listener (.toPath this) out cf)
          _   (if-not (.isAsyncStarted request)
                (.startAsync request))
          _   (.setWriteListener out wl)]
      cf))
  Object
  (service-body-async [this _ ^ServletRequest request ^ServletResponse response]
    (let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          wl  (async-writable-write-listener this out cf)
          _   (if-not (.isAsyncStarted request)
                (.startAsync request))
          _   (.setWriteListener out wl)]
      cf)))

(deftype AsyncBody [value]
  io.body/ResponseBody
  (service-body [_ servlet request response]
    (service-body-async value servlet request response)))
