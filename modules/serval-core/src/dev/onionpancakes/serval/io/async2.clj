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
  [^ServletOutputStream out ^ByteBuffer buf]
  (loop []
    (if (.hasRemaining buf)
      (do
        (.write out (.get buf))
        (if (.isReady out) (recur) false))
      true)))

;; File

(def file-ch-read-opts
  (->> [StandardOpenOption/READ]
       (into-array StandardOpenOption)))

(defn open-file-channel
  [path]
  (AsynchronousFileChannel/open path file-ch-read-opts))

(deftype AsyncFileChannelWriteListener [^ServletOutputStream out
                                        ^ByteBuffer buf
                                        pos
                                        ^AsynchronousFileChannel ch
                                        ^CompletableFuture cf]
  WriteListener
  (onWritePossible [this]
    (when (write-buffer! out buf)
      (.clear buf)
      (.read ch buf @pos nil (reify CompletionHandler
                               (completed [_ r _]
                                 (if (== r -1)
                                   (.complete cf nil)
                                   (do
                                     (.flip buf)
                                     (vswap! pos + r)
                                     (.onWritePossible this))))
                               (failed [_ throwable _]
                                 (.completeExceptionally cf throwable))))))
  (onError [_ throwable]
    (.completeExceptionally cf throwable)))

(def default-buffer-size 4092)

(defn file-write-listener
  ([out path cf]
   (file-write-listener out path cf default-buffer-size))
  ([out path cf buffer-size]
   (let [buf (doto (ByteBuffer/allocate buffer-size)
               (.limit 0)) ;; Make buffer empty.
         pos (volatile! 0)
         ch  (open-file-channel path)]
     (AsyncFileChannelWriteListener. out buf pos ch cf))))

;; Async Body

(defprotocol ResponseBodyAsync
  (service-body-async [this servlet request response]))

(extend-protocol ResponseBodyAsync
  java.nio.file.Path
  (service-body-async [this _ ^ServletRequest request ^ServletResponse response]
    (let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          wl  (file-write-listener out this cf)
          _   (if-not (.isAsyncStarted request)
                (.startAsync request))
          _   (.setWriteListener out wl)]
      cf))
  java.io.File
  (service-body-async [this _ ^ServletRequest request ^ServletResponse response]
    (let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          wl  (file-write-listener out (.toPath this) cf)
          _   (if-not (.isAsyncStarted request)
                (.startAsync request))
          _   (.setWriteListener out wl)]
      cf)))

(deftype AsyncBody [value]
  io.body/ResponseBody
  (service-body [_ servlet request response]
    (service-body-async value servlet request response)))
