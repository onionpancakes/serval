(ns dev.onionpancakes.serval.impl.http.filter
  (:refer-clojure :exclude [filter])
  (:require [dev.onionpancakes.serval.impl.http.servlet-request
             :as impl.http.request]
            [dev.onionpancakes.serval.response.http
             :as response.http])
  (:import [jakarta.servlet GenericFilter FilterChain]))

(defn context
  [filter request response filter-chain]
  {:serval.context/filter       filter
   :serval.context/request      (impl.http.request/servlet-request-proxy request)
   :serval.context/response     response
   :serval.context/filter-chain filter-chain})

(defn do-filter-chain
  [^FilterChain filter-chain m default-request default-response]
  (if-let [do-chain (:serval.filter/do-filter-chain m)]
    (if (vector? do-chain)
      (.doFilter filter-chain (nth do-chain 0) (nth do-chain 1))
      (.doFilter filter-chain default-request default-response))))

(defn service-fn
  ([handler]
   (fn [filter request response ^FilterChain filter-chain]
     (let [ctx (context filter request response filter-chain)
           ret (handler ctx)]
       (response.http/set-response response ret)
       (do-filter-chain filter-chain ret request response))))
  ([handler a]
   (fn [filter request response ^FilterChain filter-chain]
     (let [ctx (context filter request response filter-chain)
           ret (handler ctx a)]
       (response.http/set-response response ret)
       (do-filter-chain filter-chain ret request response))))
  ([handler a b]
   (fn [filter request response ^FilterChain filter-chain]
     (let [ctx (context filter request response filter-chain)
           ret (handler ctx a b)]
       (response.http/set-response response ret)
       (do-filter-chain filter-chain ret request response))))
  ([handler a b c]
   (fn [filter request response ^FilterChain filter-chain]
     (let [ctx (context filter request response filter-chain)
           ret (handler ctx a b c)]
       (response.http/set-response response ret)
       (do-filter-chain filter-chain ret request response))))
  ([handler a b c d]
   (fn [filter request response ^FilterChain filter-chain]
     (let [ctx (context filter request response filter-chain)
           ret (handler ctx a b c d)]
       (response.http/set-response response ret)
       (do-filter-chain filter-chain ret request response))))
  ([handler a b c d & args]
   (fn [filter request response ^FilterChain filter-chain]
     (let [ctx (context filter request response filter-chain)
           ret (apply handler ctx a b c d args)]
       (response.http/set-response response ret)
       (do-filter-chain filter-chain ret request response)))))

(defn filter
  ^GenericFilter
  [handler & args]
  (-> (proxy [GenericFilter] [])
      (init-proxy {"doFilter" (apply service-fn handler args)})))
