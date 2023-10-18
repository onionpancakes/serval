(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map when])
  (:require [dev.onionpancakes.serval.impl.http.servlet :as impl.http.servlet]))

;; Servlet

(defn http-servlet
  "Creates a http Servlet from a handler function."
  ^jakarta.servlet.GenericServlet
  [handler]
  (impl.http.servlet/http-servlet handler))

;; Processors

;; Process step functions is conceived as an
;; alternative to middleware or interceptors.

;; They take inspiration from transducers,
;; where the function may transform input value
;; and may pass the result to the next process step.

;; Unlike middleware, inputs values are processed
;; in a linear series of transforms.
;; i.e. There is only a 'enter' and no 'exit' transform.

;; This is possible because Serval request and response
;; data are colocated into a single context map.

(defn map
  "Returns a process step function, applying
  handler f with input and the supplied args, and
  passing the result to the next process step."
  [f & args]
  (fn [handler]
    (fn [input]
      (handler (apply f input args)))))

(defn terminate
  "Returns a process step function, applying
  handler f with input and the supplied args if
  pred of the input returns logical true. If pred
  returns logical true, the transformed result is
  not passed to the next process step, effectively
  terminating the input as this process step. If
  pred returns logical false, the input is passed
  untouched to the next process step."
  [pred f & args]
  (fn [handler]
    (fn [input]
      (if (pred input)
        (apply f input args)
        (handler input)))))

(defn when
  "Returns a process step function, applying
  handler f with input and the supplied args if
  pred of the input returns logical true, and
  passing the result to the next process step. If
  the pred returns logical false, the input is passed
  untouched to the next process step."
  [pred f & args]
  (fn [handler]
    (fn [input]
      (if (pred input)
        (handler (apply f input args))
        (handler input)))))

;; Handler

(def handler-xf
  (clojure.core/map #(% identity)))

(defn handler
  "Convert process step functions into a handler.
  When given multiple functions, they are composed
  such that the functions are called from left to right."
  [& xfs]
  (->> (into '() handler-xf xfs)
       (apply comp)))
