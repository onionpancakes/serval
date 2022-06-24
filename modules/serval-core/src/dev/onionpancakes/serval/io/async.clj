(ns dev.onionpancakes.serval.io.async
  (:require [dev.onionpancakes.serval.io.body :as io.body])
  (:import [jakarta.servlet
            ServletRequest
            ServletResponse ServletOutputStream WriteListener]
           [java.nio ByteBuffer]
           [java.util LinkedList]
           [java.util.concurrent CompletableFuture]))

;; Consider: rework to handle other non bytebuffer cases.

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

  ;; TODO: ISeq?

  ;; List of ByteBuffers only.
  java.util.List
  (to-buffer-queue [this]
    (LinkedList. this)))

(defn flush-buffer!
  "Write bytes from ByteBuffer to ServletOutputStream until
  either ByteBuffer is depleted or outputstream is unable to
  to accept more data. Returns true if outputstream remains
  ready or false if not."
  [^ServletOutputStream out ^ByteBuffer buffer]
  (loop []
    (if (.hasRemaining buffer)
      (do
        (.write out (.get buffer))
        (if (.isReady out) (recur) false))
      true)))

(defn flush-buffer-queue!
  "Write bytes from a queue of ByteBuffers to a ServletOutputStream
  until either the queue is depleted or outputstream is unable to
  accept more data. Returns true if outputstream remains ready or
  false if not."
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
    (if (flush-buffer-queue! out queue)
      (.complete complete nil)))
  (onError [_ throwable]
    (.completeExceptionally complete throwable)))

(defn queue-write-listener
  [out queue cf]
  (QueueWriteListener. out queue cf))

(defn service-buffer-queue
  [queue _ ^ServletRequest request ^ServletResponse response]
  (let [out (.getOutputStream response)
        cf  (CompletableFuture.)
        wl  (queue-write-listener out queue cf)
        _   (if-not (.isAsyncStarted request)
              (.startAsync request))
        _   (.setWriteListener out wl)]
    cf))

;; Consider: should the conversion from AsyncWritable to queue
;; be eager or lazy?

(deftype AsyncBody [queue]
  io.body/ResponseBody
  (service-body [_ servlet request response]
    (service-buffer-queue queue servlet request response)))

(defn async-body [value]
  (AsyncBody. (to-buffer-queue value)))
