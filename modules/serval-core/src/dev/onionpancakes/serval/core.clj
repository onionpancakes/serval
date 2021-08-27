(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map merge])
  (:require [dev.onionpancakes.serval.io.http :as io.http])
  (:import [jakarta.servlet GenericServlet]))

;; Servlet

(defn servlet*
  [service-fn]
  (proxy [GenericServlet] []
    (service [request response]
      (service-fn this request response))))

;; Http Servlet

(defn http-service-fn
  [handler]
  (fn [servlet request response]
    (let [ctx (io.http/context servlet request response)]
      (io.http/write-response! ctx (handler ctx)))))

(defn http-servlet
  [handler]
  (servlet* (http-service-fn handler)))

;; Middleware

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

(defn merge
  [m]
  (fn [ctx]
    (conj ctx m)))
