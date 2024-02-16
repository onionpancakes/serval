(ns dev.onionpancakes.serval.impl.filter
  (:refer-clojure :exclude [filter])
  (:require [dev.onionpancakes.serval.impl.servlet-request :as impl.request]))

(deftype ServalFilter [^:volatile-mutable filter-config
                        do-filter-fn
                        destroy-fn]
  jakarta.servlet.Filter
  (init [_ config]
    (set! filter-config config))
  (doFilter [this request response chain]
    (do-filter-fn this request response chain))
  (destroy [this]
    (if (some? destroy-fn)
      (destroy-fn this))))

(defn wrapping-do-filter-fn
  [do-filter-fn]
  (fn [this request response chain]
    (do-filter-fn this
                  (impl.request/servlet-request-proxy request)
                  response
                  chain)))

(defn filter*
  [do-filter-fn]
  (ServalFilter. nil do-filter-fn nil))

(defn filter
  [do-filter-fn]
  (ServalFilter. nil (wrapping-do-filter-fn do-filter-fn) nil))
