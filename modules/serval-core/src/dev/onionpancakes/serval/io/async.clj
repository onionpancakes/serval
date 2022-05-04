(ns dev.onionpancakes.serval.io.async
  (:require [dev.onionpancakes.serval.io.body :as io.body])
  (:import [jakarta.servlet
            ServletRequest
            ServletResponse ServletOutputStream WriteListener]
           [java.nio ByteBuffer]
           [java.util LinkedList]
           [java.util.concurrent CompletableFuture]))

(defprotocol AsyncWritable
  (to-buffer-queue [this]))

(extend-protocol AsyncWritable
  (Class/forName "[B")
  (to-buffer-queue [this]
    (doto (LinkedList.)
      (.add (ByteBuffer/wrap this))))
  String
  (to-buffer-queue [this]
    (doto (LinkedList.)
      (.add (ByteBuffer/wrap (.getBytes this)))))
  ByteBuffer
  (to-buffer-queue [this]
    (doto (LinkedList.)
      (.add this)))
  java.util.List
  (to-buffer-queue [this]
    (LinkedList. this)))

(defn flush-buffer!
  [^ServletOutputStream out ^ByteBuffer buffer]
  (loop []
    (if (.hasRemaining buffer)
      (do
        (.write out (.get buffer))
        (if (.isReady out) (recur) false))
      true)))

(defn flush-buffer-queue!
  "Write bytes from a queue of ByteBuffers to a ServletOutputStream,
  so long as out remains ready.
  Returns true if flush should continue, else false."
  [out ^java.util.Queue buffers]
  (loop []
    (if-some [buf (.peek buffers)]
      (if (flush-buffer! out buf)
        (do (.remove buffers) (recur))
        false)
      true)))

(deftype QueueWriteListener [out queue ^CompletableFuture complete]
  WriteListener
  (onWritePossible [_]
    (if-not (flush-buffer-queue! out queue)
      (.complete complete nil)))
  (onError [_ throwable]
    (.completeExceptionally complete throwable)))

(defn queue-write-listener
  [out queue cf]
  (QueueWriteListener. out queue cf))

(defn service-buffer-queue
  [queue _ ^ServletRequest request ^ServletResponse response]
  (if-not (.isAsyncStarted request)
    (.startAsync request))
  (let [out (.getOutputStream response)
        cf  (CompletableFuture.)
        wl  (queue-write-listener out queue cf)
        _   (.setWriteListener out wl)]
    cf))

(deftype AsyncBody [queue]
  io.body/ResponseBody
  (service-body [this servlet request response]
    (service-buffer-queue queue servlet request response)))

(defn async-body [value]
  (AsyncBody. (to-buffer-queue value)))
