(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map])
  (:require [dev.onionpancakes.serval.io.body :as io.body]
            [dev.onionpancakes.serval.io.http :as io.http]
            [dev.onionpancakes.serval.io.http2 :as io.http2])
  (:import [jakarta.servlet GenericServlet]))

;; Async

(defn enable-core-async! []
  (require 'dev.onionpancakes.serval.io.async))

(def async-body
  io.body/async-body)

;; Servlet

(defn servlet*
  [service-fn]
  (proxy [GenericServlet] []
    (service [request response]
      (service-fn this request response))))

(defn http-servlet
  [handler]
  (servlet* (io.http/service-fn handler)))

(defn http-servlet2
  [handler]
  (servlet* (io.http2/service-fn handler)))

;; Processors

(defn map
  [f & args]
  (fn [handler]
    (fn [input]
      (handler (apply f input args)))))

(defn terminate
  [pred f & args]
  (fn [handler]
    (fn [input]
      (if (pred input)
        (apply f input args)
        (handler input)))))

;; Handler

(def handler-xf
  (clojure.core/map #(% identity)))

(defn handler
  [& xfs]
  (->> (into '() handler-xf xfs)
       (apply comp)))
