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
  (write! [this out]))

(extend-protocol AsyncWritable
  ByteBuffer
  (write! [this out]
    (write-buffer! this out)))

(defprotocol AsyncWritableValue
  (writable [this]))

(extend-protocol AsyncWritableValue
  (Class/forName "[B")
  (writable [this]
    (ByteBuffer/wrap this)))

;; File write listener

(deftype AsyncFileChannelWriteListener [^ServletOutputStream out
                                        ^ByteBuffer buf
                                        ^:volatile-mutable ^long pos
                                        ^AsynchronousFileChannel ch
                                        ^CompletableFuture cf]
  ;; Impl both WriteListener and CompletionHandler
  ;; on the same type, so both can have access to a private
  ;; volatile-mutable unboxed long pos variable.
  ;; (Premature) optimization lol?
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
        (.onWritePossible this))))
  (failed [_ throwable _]
    (.completeExceptionally cf throwable)))

(def default-buffer-size 4092)

(def default-file-channel-opts
  (->> [StandardOpenOption/READ]
       (into-array StandardOpenOption)))

(defn async-file-channel-write-listener
  ([out path cf]
   (file-write-listener out path cf default-buffer-size))
  ([out path cf buffer-size]
   (let [buf (doto (ByteBuffer/allocate buffer-size)
               (.limit 0)) ;; Make buffer empty.
         ch  (AsynchronousFileChannel/open path default-file-channel-opts)]
     (AsyncFileChannelWriteListener. out buf 0 ch cf))))

;; Async Body

(defprotocol ResponseBodyAsync
  (service-body-async [this servlet request response]))

(extend-protocol ResponseBodyAsync
  java.nio.file.Path
  (service-body-async [this _ ^ServletRequest request ^ServletResponse response]
    (let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          wl  (async-file-channel-write-listener out this cf)
          _   (if-not (.isAsyncStarted request)
                (.startAsync request))
          _   (.setWriteListener out wl)]
      cf))
  java.io.File
  (service-body-async [this _ ^ServletRequest request ^ServletResponse response]
    (let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          wl  (async-file-channel-write-listener out (.toPath this) cf)
          _   (if-not (.isAsyncStarted request)
                (.startAsync request))
          _   (.setWriteListener out wl)]
      cf)))

(deftype AsyncBody [value]
  io.body/ResponseBody
  (service-body [_ servlet request response]
    (service-body-async value servlet request response)))
