(ns dev.onionpancakes.serval.io.body2
  (:import [jakarta.servlet
            ServletRequest
            ServletResponse ServletOutputStream
            WriteListener]
           [java.nio ByteBuffer]
           [java.util LinkedList]
           [java.util.concurrent
            CompletionStage CompletableFuture
            Flow$Publisher Flow$Subscriber Flow$Subscription]
           [java.util.function Function]))

#_(defrecord FlowContext [^ServletOutputStream out
                        ^java.util.Queue queue
                        ^java.util.Queue current
                        ^boolean complete?])

#_(defprotocol FlowSubscriberContext
  (on-subscribe! [this sub])
  (on-next! [this input])
  (on-complete! [this])
  (process-context! [this]))

#_(deftype ListOfBufferFlowSubscriberContext [^:unsynchronized-mutable ^Flow$Subscription subscription
                                            ^java.util.Queue queue
                                            ^:unsynchronized-mutable ^boolean complete?]
  FlowSubscriberContext
  (on-subscribe! [_ sub]
    (if subscription
      (.cancel subscription))
    (set! subscription sub))
  (on-next! [this buffers]
    (.add queue (LinkedList. buffers)))
  (on-complete! [this]
    (set! complete? (boolean true))))

#_(defprotocol Channel
  (take! [this]))

#_(deftype FlowSubscriberChannel [^:unsynchronized-mutable ^Flow$Subscription subscription
                                ^java.util.Queue queue
                                ^java.util.Queue buffer
                                complete?]
  Flow$Subscriber
  (onSubscribe [this sub]
    (if subscription
      (.cancel subscription))
    (set! subscription sub))
  (onNext [this input]
    (.complete (.poll queue) input))
  (onComplete [this])
  (onError [this throwable])
  Channel
  (take! [this]
    (let [cf (CompletableFuture.)]
      (.add queue cf)
      (if subscription
        (.request subscription 1))
      cf)))

(defn take! [])

(defn flush-buffers! [out buffers])

(deftype ChannelWriteListener [out ch
                               ^:unsynchronized-mutable
                               ^CompletableFuture cf]
  WriteListener
  (onWritePossible [this]
    (let [f (reify Function
              (apply [this-fn buffers]
                (if-not (nil? buffers)
                  (if (flush-buffers! out buffers)
                    (.thenComposeAsync (take! ch) this-fn)
                    (CompletableFuture/completedFuture buffers)))))]
      (set! cf (.thenCompose cf f))))
  (onError [_ throwable]
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
  (service-body [this _ _ ^ServletResponse response])

  ;; Async
  CompletionStage
  (service-body [this servlet request response]
    (.thenAccept this (reify java.util.function.Consumer
                        (accept [this value]
                          ;; Value should not be another CompletionStage.
                          (service-body value servlet request response)))))
  Flow$Publisher
  (service-body [this _ ^ServletRequest request ^ServletResponse response]
    (if-not (.isAsyncStarted request)
      (.startAsync request))
    #_(let [out (.getOutputStream response)
          cf  (CompletableFuture.)
          wl  (flow-subscriber-write-listener out [] cf false)]
      (.setWriteListener out wl)
      (.subscribe this wl)
      cf)))
