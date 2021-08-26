(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map])
  (:require [dev.onionpancakes.serval.io.http :as io.http])
  (:import [jakarta.servlet GenericServlet]))

;; Servlet

(defn http-service-fn
  [handler]
  (fn [servlet request response]
    (let [ctx (io.http/context servlet request response)]
      (io.http/write-response! ctx (handler ctx)))))

(defn servlet*
  [service-fn]
  (proxy [GenericServlet] []
    (service [request response]
      (service-fn this request response))))

(defn servlet
  [handler]
  (servlet* (http-service-fn handler)))

;; Middleware

(defn map
  [f]
  (fn [handler]
    (fn [input]
      (handler (f input)))))

(defn terminate
  [pred f]
  (fn [handler]
    (fn [input]
      (if (pred input)
        (f input)
        (handler input)))))

;; Handler

(def handler-xf
  (clojure.core/map #(% identity)))

(defn handler
  [& xfs]
  (->> (into '() handler-xf xfs)
       (apply comp)))
