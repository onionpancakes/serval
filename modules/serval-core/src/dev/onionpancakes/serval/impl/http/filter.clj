(ns dev.onionpancakes.serval.impl.http.filter
  (:refer-clojure :exclude [filter])
  (:require [dev.onionpancakes.serval.service.filter
             :as service.filter])
  (:import [jakarta.servlet GenericFilter]))

(defn context
  [servlet request response filter-chain]
  {:serval.context/servlet      servlet
   :serval.context/request      request
   :serval.context/response     response
   :serval.context/filter-chain filter-chain})

(defn service-fn
  [handler]
  (fn [servlet request response filter-chain]
    (-> (context servlet request response filter-chain)
        (handler)
        (service.filter/service-filter servlet request response filter-chain))))

(defn filter
  ^GenericFilter
  [handler]
  (-> (proxy [GenericFilter] [])
      (init-proxy {"doFilter" (service-fn handler)})))
