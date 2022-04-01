(ns dev.onionpancakes.serval.io.body2
  (:import [jakarta.servlet
            ServletResponse ServletOutputStream
            WriteListener]
           [java.nio ByteBuffer]
           [java.util.concurrent
            CompletionStage CompletableFuture
            Flow$Publisher Flow$Subscriber Flow$Subscription]))

(defn flush-buffer!
  [^ServletOutputStream out ^ByteBuffer buffer]
  (loop []
    (if (.hasRemaining buffer)
      (do
        (.write out (.get buffer))
        (if (.isReady out) (recur) false))
      true)))

(defn flush-buffers!
  [^ServletOutputStream out ^java.util.Queue buffers]
  (loop []
    (let [buf (.peek buffers)]
      (cond
        (nil? buf)                  true
        (identical? buf ::complete) false
        :default                    (if (flush-buffer! out buf)
                                      (do (.remove buffers) (recur))
                                      true)))))

(deftype FlowSubscriberWriteListener [^:unsynchronized-mutable ^Flow$Subscription subscription
                                      ^java.util.Queue buffers
                                      ^ServletOutputStream out
                                      ^CompletableFuture cf]
  Flow$Subscriber
  (onSubscribe [_ sub]
    (if subscription
      (.cancel subscription))
    (set! subscription sub))
  (onNext [this bufs]
    (locking this
      (.addAll buffers bufs)
      (if (.isReady out)
        (if-not (flush-buffers! out buffers)
          (.complete cf nil)))))
  (onComplete [this]
    (locking this
      (.add buffers ::complete)
      (if (.isReady out)
        (if-not (flush-buffers! out buffers)
          (.complete cf nil)))))
  WriteListener
  (onWritePossible [this]
    (locking this
      (if-not (flush-buffers! out buffers)
        (.complete cf nil))))
  (onError [_ throwable]
    (if subscription
      (.cancel subscription))
    (.completeExceptionally cf throwable)))

;; Response body protocol

(defprotocol ResponseBody
  (service-body [this servlet request response]))

(extend-protocol ResponseBody
  (Class/forName "[B")
  (service-body [this _ _ ^ServletResponse response]
    (.. response getOutputStream (write ^bytes this))
    nil)
  String
  (service-body [this _ _ ^ServletResponse response]
    (.. response getWriter (write this))
    nil)
  java.io.InputStream
  (service-body [this _ _ ^ServletResponse response]
    (try
      (.transferTo this (.getOutputStream response))
      (finally
        (.close this)))
    nil)
  nil
  (service-body [this _ _ ^ServletResponse response ]
    (.close (.getOutputStream response)))

  ;; Async
  CompletionStage
  (service-body [this servlet request response]
    (.thenAccept this (reify java.util.function.Consumer
                        (accept [this value]
                          ;; Value should not be another CompletionStage.
                          (service-body value servlet request response)))))
  Flow$Publisher
  (service-body [this _ _ _]
    ;; TODO
    nil))
