(ns dev.onionpancakes.serval.servlet
  (:refer-clojure :exclude [filter])
  (:require [dev.onionpancakes.serval.impl.servlet :as impl.servlet]
            [dev.onionpancakes.serval.impl.filter :as impl.filter]
            [dev.onionpancakes.serval.core :as srv])
  (:import [jakarta.servlet FilterChain]
           [jakarta.servlet.http HttpServletResponse]))

(defn filter
  {:tag jakarta.servlet.Filter}
  [handler]
  (impl.filter/handler-filter handler))

(defn servlet
  {:tag jakarta.servlet.Servlet}
  [handler]
  (impl.servlet/handler-servlet handler))

;; Filters

;; TODO remove
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

(defn pred-filter-handler
  ([pred code]
   (fn [ctx]
     (if (pred ctx)
       (srv/do-filter ctx)
       (srv/send-error ctx code))))
  ([pred code message]
   (fn [ctx]
     (if (pred ctx)
       (srv/do-filter ctx)
       (srv/send-error ctx code message)))))

(defn pred-filter
  {:tag jakarta.servlet.Filter}
  ([pred code]
   (filter (pred-filter-handler pred code)))
  ([pred code message]
   (filter (pred-filter-handler pred code message))))

(defn http-method-filter
  {:tag jakarta.servlet.Filter}
  ([allowed-method?]
   (http-method-filter allowed-method? 405))
  ([allowed-method? code]
   (-> (comp allowed-method? :method :serval.context/request)
       (pred-filter code)))
  ([allowed-method? code message]
   (-> (comp allowed-method? :method :serval.context/request)
       (pred-filter code message))))
