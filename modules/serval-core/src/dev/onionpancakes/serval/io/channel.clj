(ns dev.onionpancakes.serval.io.channel
  (:import [java.util.concurrent
            CompletionStage CompletableFuture
            Flow$Publisher Flow$Subscriber Flow$Subscription]))

(defprotocol ReadChannel
  (take! ^CompletionStage [this]))

(deftype FlowSubscriberChannel [^:unsynchronized-mutable
                                ^Flow$Subscription subscription
                                ^java.util.Queue buffer
                                ^long buffer-fill-size
                                ^java.util.Queue pending
                                ^long pending-max-size
                                ^:unsynchronized-mutable
                                ^boolean closed?
                                ex-handler]
  Flow$Subscriber
  (onSubscribe [this sub]
    (locking this
      (if subscription
        (.cancel subscription))
      (set! subscription sub)
      (if (< (.size buffer) buffer-fill-size)
        (.request subscription 1))))
  (onNext [this input]
    (locking this
      (when-not closed?
        (if (.peek pending)
          (.complete ^CompletableFuture (.poll pending) input)
          (.add buffer input))
        (if (< (.size buffer) buffer-fill-size)
          (.request subscription 1)))))
  (onComplete [this]
    (locking this
      (set! closed? (boolean true))))
  (onError [this throwable]
    (ex-handler throwable))
  ReadChannel
  (take! [this]
    (locking this
      (if-let [val (.poll buffer)]
        (CompletableFuture/completedFuture val)
        (if (>= (.size pending) pending-max-size)
          (throw (ex-info "Exceeded max pending queue size."
                          {:pending-max-size pending-max-size}))
          (if closed?
            (CompletableFuture/completedFuture nil)
            (let [cf (CompletableFuture.)]
              (.add pending cf)
              cf)))))))

(defn flow-subscriber-channel
  [n ex-handler]
  (let [buffer (java.util.LinkedList.)
        queue  (java.util.LinkedList.)]
    (FlowSubscriberChannel. nil buffer n queue 1024 false ex-handler)))
