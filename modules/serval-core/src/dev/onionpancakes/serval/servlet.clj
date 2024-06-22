(ns dev.onionpancakes.serval.servlet
  (:refer-clojure :exclude [filter])
  (:require [dev.onionpancakes.serval.impl.servlet :as impl.servlet]
            [dev.onionpancakes.serval.impl.filter :as impl.filter])
  (:import [jakarta.servlet FilterChain]
           [jakarta.servlet.http HttpServletResponse]))

(defn filter
  {:tag jakarta.servlet.Filter}
  [do-filter-fn]
  (impl.filter/filter do-filter-fn))

(defn servlet
  {:tag jakarta.servlet.Servlet}
  [service-fn]
  (impl.servlet/servlet service-fn))

;; Filters

(defn pred-do-filter-fn
  ([pred code]
   (fn [_ request ^HttpServletResponse response ^FilterChain chain]
     (if (pred request)
       (.doFilter chain request response)
       (.sendError response code))))
  ([pred code message]
   (fn [_ request ^HttpServletResponse response ^FilterChain chain]
     (if (pred request)
       (.doFilter chain request response)
       (.sendError response code message)))))

(defn pred-filter
  {:tag jakarta.servlet.Filter}
  ([pred code]
   (impl.filter/filter (pred-do-filter-fn pred code)))
  ([pred code message]
   (impl.filter/filter (pred-do-filter-fn pred code message))))

(defn http-method-filter
  {:tag jakarta.servlet.Filter}
  ([allowed-method?]
   (http-method-filter allowed-method? 405))
  ([allowed-method? code]
   (-> (comp allowed-method? :method)
       (pred-filter code)))
  ([allowed-method? code message]
   (-> (comp allowed-method? :method)
       (pred-filter code message))))
