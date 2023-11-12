(ns dev.onionpancakes.serval.service.filter
  (:require [dev.onionpancakes.serval.service.http
             :as service.http])
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function BiConsumer]
           [jakarta.servlet FilterChain]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse]))

(defn async-filter?
  [m]
  (service.http/async-response? m))

(defn set-filter
  ^CompletionStage
  [m servlet request response ^FilterChain filter-chain]
  (let [next-cs (if (contains? m :serval.filter/next)
                  (-> (:serval.filter/next m) ; TODO filter next protocol
                      (CompletableFuture/completedFuture)))]
    (cond-> (service.http/set-response m servlet request response)
      next-cs (.thenAcceptBoth next-cs (reify BiConsumer
                                         (accept [_ _ do-filter]
                                           (if do-filter
                                             (.doFilter filter-chain request response))))))))

(defn complete-filter
  [^CompletionStage stage _ ^HttpServletRequest request ^HttpServletResponse response _]
  (.whenComplete stage (reify BiConsumer
                         (accept [_ _ throwable]
                           (if throwable
                             (->> (.getMessage ^Throwable throwable)
                                  (.sendError response 500)))
                           (if (.isAsyncStarted request)
                             (.. request (getAsyncContext) (complete)))))))

(defn service-filter
  [m servlet ^HttpServletRequest request response filter-chain]
  (let [_ (if (and (async-filter? m)
                   (not (.isAsyncStarted request)))
            (.startAsync request))]
    (-> (set-filter m servlet request response filter-chain)
        (complete-filter servlet request response filter-chain))))
