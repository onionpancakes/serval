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

(defn pred-filter-handler
  ([ctx pred code]
   (if (pred ctx)
     (srv/do-filter-chain! ctx)
     (srv/send-error! ctx code)))
  ([ctx pred code message]
   (if (pred ctx)
     (srv/do-filter-chain! ctx)
     (srv/send-error! ctx code message))))

(defn pred-filter
  (^jakarta.servlet.GenericFilter
   [pred code]
   (filter pred-filter-handler pred code))
  (^jakarta.servlet.GenericFilter
   [pred code message]
   (filter pred-filter-handler pred code message)))

(defn http-method-filter
  (^jakarta.servlet.GenericFilter
   [allowed-method?]
   (http-method-filter allowed-method? 405))
  (^jakarta.servlet.GenericFilter
   [allowed-method? code]
   (-> (comp allowed-method? :method :serval.context/request)
       (pred-filter code)))
  (^jakarta.servlet.GenericFilter
   [allowed-method? code message]
   (-> (comp allowed-method? :method :serval.context/request)
       (pred-filter code message))))
