(ns dev.onionpancakes.serval.core-async
  (:require [clojure.core.async :as async :refer [go-loop <! >!]]
            [dev.onionpancakes.serval.io.body :as io.body]
            [dev.onionpancakes.serval.io.async :as io.async])
  (:import [jakarta.servlet ServletRequest ServletResponse WriteListener]
           [java.util.concurrent CompletableFuture]))

;; 'cur' is a 1 size channel that holds the current writable.

(deftype ChannelWriteListener [ch cur out ^CompletableFuture cf]
  WriteListener
  (onWritePossible [_]
    (go-loop [buf (<! cur)]
      (if (some? buf)
        (if (io.async/write! buf out)
          (if-some [v (<! ch)]
            (recur (io.async/writable v))
            (recur nil))
          (>! cur buf))
        (do
          (async/close! cur)
          (.complete cf nil)))))
  (onError [_ throwable]
    (.completeExceptionally cf throwable)))

(defn channel-write-listener
  [ch out cf]
  (let [cur (async/chan 1)
        ;; Because ChannelWriteListener takes from 'cur' first,
        ;; we need to initialize it with one value from ch.
        cb  (fn [x]
              (if (some? x)
                (async/put! cur (io.async/writable x))
                (async/close! cur)))
        _   (async/take! ch cb)]
    (ChannelWriteListener. ch cur out cf)))

;; Protocol impl

(defn service-channel-body
  [ch _ ^ServletRequest request ^ServletResponse response]
  (let [out (.getOutputStream response)
        cf  (CompletableFuture.)
        wl  (channel-write-listener ch out cf)
        _   (if-not (.isAsyncStarted request)
              (.startAsync request))
        _   (.setWriteListener out wl)]
    cf))

(deftype ChannelBody [ch]
  io.body/ResponseBody
  (service-body [_ servlet request response]
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
