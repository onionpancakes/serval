(ns dev.onionpancakes.serval.io.async
  (:require [clojure.core.async :as async :refer [go-loop <! >!]]
            [dev.onionpancakes.serval.io.body2 :as io.body])
  (:import [jakarta.servlet
            ServletRequest ServletResponse
            ServletOutputStream WriteListener]
           [java.nio ByteBuffer]
           [java.util LinkedList]
           [java.util.concurrent CompletableFuture]))

;; Async writables

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

;; Write listener

(defn flush-buffer!
  [^ServletOutputStream out ^ByteBuffer buffer]
  (loop []
    (if (.hasRemaining buffer)
      (do
        (.write out (.get buffer))
        (if (.isReady out) (recur) false))
      true)))

(defn flush-buffer-queue!
  [out ^java.util.Queue buffers]
  (loop []
    (if-some [buf (.peek buffers)]
      (if (flush-buffer! out buf)
        (do (.remove buffers) (recur))
        false)
      true)))

;; The 'current' is a 1 size channel that holds the current buffer queue
;; being written out.
;; Buffer queues are taken from 'current' then 'input', and flushed to
;; the ServletOutputStream.
;; When this current flush reaches its limit (i.e. isReady() returns false),
;; the current partially flushed buffer queue is place back onto 'current'
;; to await the next 'onWritePossible' call.

(deftype ChannelWriteListener [out current input ^CompletableFuture complete]
  WriteListener
  (onWritePossible [_]
    (go-loop [buffers (<! current)]
      (if (nil? buffers)
        (do
          (async/close! current)
          (.complete complete nil))
        (if (flush-buffer-queue! out buffers)
          (recur (<! input))
          (>! current buffers)))))
  (onError [_ throwable]
    (.completeExceptionally complete throwable)))

(defn channel-write-listener
  [out ch cf]
  (let [cur (async/chan 1)
        ;; Because ChannelWriteListener takes from 'current' first,
        ;; we need to initialize it with one value from ch.
        _   (async/take! ch #(if (nil? %)
                               (async/close! cur)
                               (async/put! cur %)))]
    (ChannelWriteListener. out cur ch cf)))

;; Protocol impl

(def to-buffer-queue-xf
  (map to-buffer-queue))

(extend-protocol io.body/ResponseBody
  clojure.core.async.impl.channels.ManyToManyChannel
  (service-body [this _ ^ServletRequest request ^ServletResponse response]
    (if-not (.isAsyncStarted request)
      (.startAsync request))
    (let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          ex  (fn [t]
                ;; Close this channel on error.
                ;; Otherwise, it will feed into a completed async response.
                (async/close! this)
                (.completeExceptionally cf t) nil)
          ch  (async/chan 32 to-buffer-queue-xf ex)
          wl  (channel-write-listener out ch cf)]
      (.setWriteListener out wl)
      (async/pipe this ch)
      cf)))
