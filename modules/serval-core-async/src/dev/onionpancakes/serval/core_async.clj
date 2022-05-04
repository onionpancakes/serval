(ns dev.onionpancakes.serval.core-async
  (:require [clojure.core.async :as async :refer [go-loop <! >!]]
            [dev.onionpancakes.serval.io.body :as io.body]
            [dev.onionpancakes.serval.io.async :as io.async])
  (:import [jakarta.servlet
            ServletRequest ServletResponse
            ServletOutputStream WriteListener]
           [java.nio ByteBuffer]
           [java.util LinkedList]
           [java.util.concurrent CompletableFuture]))

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
        (if (io.async/flush-buffer-queue! out buffers)
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
  (map io.async/to-buffer-queue))

(defn service-channel-body
  [ch _ ^ServletRequest request ^ServletResponse response]
  (let [out (.getOutputStream response)
        cf  (CompletableFuture.)
        ex  (fn [t]
              ;; Close this channel on error.
              ;; Otherwise, it will feed into a completed async response.
              (async/close! ch)
              (.completeExceptionally cf t) nil)
        qch (async/chan 32 to-buffer-queue-xf ex)
        wl  (channel-write-listener out qch cf)
        _   (if-not (.isAsyncStarted request)
              (.startAsync request))
        _   (.setWriteListener out wl)
        _   (async/pipe ch qch)]
    cf))

(deftype ChannelBody [ch]
  io.body/ResponseBody
  (service-body [this servlet request response]
    (service-channel-body ch servlet request response)))

(defn channel-body
  [ch]
  (ChannelBody. ch))

(defn extend-channel-as-response-body!
  []
  (extend-protocol io.body/ResponseBody
    clojure.core.async.impl.channels.ManyToManyChannel
    (service-body [this servlet request response]
      (service-channel-body this servlet request response))))
