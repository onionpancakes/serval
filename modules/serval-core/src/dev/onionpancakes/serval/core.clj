(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map])
  (:require [dev.onionpancakes.serval.io.http :as io.http])
  (:import [jakarta.servlet GenericServlet]))

;; Servlet

(def default-service-fn-opts
  {:read-fn  io.http/read-context
   :write-fn io.http/write-context!})

(defn service-fn
  ([handler]
   (service-fn handler default-service-fn-opts))
  ([handler {:keys [read-fn write-fn]}]
   (fn [servlet request response]
     (->> (read-fn servlet request response)
          (handler)
          (write-fn servlet request response)))))

(defn servlet*
  [service-fn]
  (proxy [GenericServlet] []
    (service [request response]
      (service-fn this request response))))

(defn servlet
  ([handler]
   (servlet* (service-fn handler)))
  ([handler opts]
   (servlet* (service-fn handler opts))))

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
