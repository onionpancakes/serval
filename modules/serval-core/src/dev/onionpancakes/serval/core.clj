(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map])
  (:require [dev.onionpancakes.serval.io.http :as io.http]
            [dev.onionpancakes.serval.io.async :as io.async])
  (:import [jakarta.servlet Servlet GenericServlet]))

;; Servlet

(defn ^Servlet generic-servlet
  [service-fn]
  (proxy [GenericServlet] []
    (service [request response]
      (service-fn this request response))))

(defn ^Servlet http-servlet
  [handler]
  (generic-servlet (io.http/service-fn handler)))

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

;; Async

(def async-body
  io.async/async-body)
