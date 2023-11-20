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
  [do-chain ^FilterChain filter-chain default-request default-response]
  (if do-chain
    (if (vector? do-chain)
      (.doFilter filter-chain (nth do-chain 0) (nth do-chain 1))
      (.doFilter filter-chain default-request default-response))))

(defn service-fn
  [handler]
  (fn [filter request response ^FilterChain filter-chain]
    (let [ctx (context filter request response filter-chain)
          ret (handler ctx)]
      (response.http/set-response response ret)
      (-> (:serval.filter/do-filter-chain ret)
          (do-filter-chain filter-chain request response)))))

(defn filter
  ^GenericFilter
  [handler]
  (-> (proxy [GenericFilter] [])
      (init-proxy {"doFilter" (service-fn handler)})))
