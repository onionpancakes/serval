(ns dev.onionpancakes.serval.servlet
  (:refer-clojure :exclude [filter])
  (:require [dev.onionpancakes.serval.impl.http.servlet :as impl.http.servlet]
            [dev.onionpancakes.serval.impl.http.filter :as impl.http.filter]
            [dev.onionpancakes.serval.core :as srv]))

(defn servlet
  "Creates a Servlet which services a http response from the handler function."
  ^jakarta.servlet.GenericServlet
  [handler & args]
  (apply impl.http.servlet/servlet handler args))

(defn filter
  ^jakarta.servlet.GenericFilter
  [handler & args]
  (apply impl.http.filter/filter handler args))

;; Filters

(defn pred-filter
  ([pred code]
   (filter (fn [ctx]
             (if (pred ctx)
               (srv/do-filter-chain! ctx)
               (srv/send-error! ctx code))
             nil)))
  ([pred code message]
   (filter (fn [ctx]
             (if (pred ctx)
               (srv/do-filter-chain! ctx)
               (srv/send-error! ctx code message))
             nil))))

(defn http-method-filter
  [allowed-method?]
  (let [pred (comp allowed-method? :method :serval.context/request)]
    (pred-filter pred 405)))
