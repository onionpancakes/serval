(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map when])
  (:import [jakarta.servlet FilterChain]))

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
  [& steps]
  (->> (into '() handler-xf steps)
       (apply comp)))

;; Handlers

(defn response
  "Sets response status, body, content-type, and character-encoding."
  ([ctx status]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status status))
  ([ctx status body]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status status
              :serval.response/body   body))
  ([ctx status body content-type]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status       status
              :serval.response/body         body
              :serval.response/content-type content-type))
  ([ctx status body content-type character-encoding]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status             status
              :serval.response/body               body
              :serval.response/content-type       content-type
              :serval.response/character-encoding character-encoding)))

(defn do-filter-chain
  "Sets do-filter-chain, which is processed when the filter completes.
  Since do-filter-chain is processed after filter completion,
  this only allows for filter pre-processing."
  ([ctx]
   (assoc ctx :serval.filter/do-filter-chain true))
  ([ctx do-chain]
   (assoc ctx :serval.filter/do-filter-chain do-chain)))

(defn do-filter-chain!
  "Does the filter chain immediately. Allows for filter post-processing."
  ([{:serval.context/keys [request response ^FilterChain filter-chain] :as ctx}]
   (.doFilter filter-chain request response)
   ctx)
  ([{:serval.context/keys [^FilterChain filter-chain] :as ctx} request response]
   (.doFilter filter-chain request response)
   ctx))
