(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map when])
  (:require [dev.onionpancakes.serval.response.body
             :as response.body]
            [dev.onionpancakes.serval.response.http
             :as response.http])
  (:import [jakarta.servlet FilterChain ServletRequest ServletResponse
            ServletInputStream ServletOutputStream]
           [jakarta.servlet.http HttpServletResponse]))

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
  ([f]
   (fn [handler]
     (fn [input]
       (handler (f input)))))
  ([f a]
   (fn [handler]
     (fn [input]
       (handler (f input a)))))
  ([f a b]
   (fn [handler]
     (fn [input]
       (handler (f input a b)))))
  ([f a b c]
   (fn [handler]
     (fn [input]
       (handler (f input a b c)))))
  ([f a b c d]
   (fn [handler]
     (fn [input]
       (handler (f input a b c d)))))
  ([f a b c d & args]
   (fn [handler]
     (fn [input]
       (handler (apply f input a b c d args))))))

(defn terminate
  "Returns a process step function, applying
  handler f with input and the supplied args if
  pred of the input returns logical true. If pred
  returns logical true, the transformed result is
  not passed to the next process step, effectively
  terminating the input as this process step. If
  pred returns logical false, the input is passed
  untouched to the next process step."
  ([pred f]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (f input)
         (handler input)))))
  ([pred f a]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (f input a)
         (handler input)))))
  ([pred f a b]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (f input a b)
         (handler input)))))
  ([pred f a b c]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (f input a b c)
         (handler input)))))
  ([pred f a b c d]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (f input a b c d)
         (handler input)))))
  ([pred f a b c d & args]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (apply f input a b c d args)
         (handler input))))))

(defn when
  "Returns a process step function, applying
  handler f with input and the supplied args if
  pred of the input returns logical true, and
  passing the result to the next process step. If
  the pred returns logical false, the input is passed
  untouched to the next process step."
  ([pred f]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (handler (f input))
         (handler input)))))
  ([pred f a]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (handler (f input a))
         (handler input)))))
  ([pred f a b]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (handler (f input a b))
         (handler input)))))
  ([pred f a b c]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (handler (f input a b c))
         (handler input)))))
  ([pred f a b c d]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (handler (f input a b c d))
         (handler input)))))
  ([pred f a b c d & args]
   (fn [handler]
     (fn [input]
       (if (pred input)
         (handler (apply f input a b c d args))
         (handler input))))))

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

(defn headers
  "Sets the response headers."
  [ctx headers]
  (assoc ctx :serval.response/headers headers))

(defn set-headers!
  "Sets the response headers immediately.

  Context is unchanged."
  [{:serval.context/keys [response] :as ctx} headers]
  (response.http/set-headers response headers)
  ctx)

(defn body
  "Sets the response body, content-type, and character-encoding."
  ([ctx body]
   (assoc ctx :serval.response/body body))
  ([ctx body content-type]
   (assoc ctx
          :serval.response/body         body
          :serval.response/content-type content-type))
  ([ctx body content-type character-encoding]
   (assoc ctx
          :serval.response/body               body
          :serval.response/content-type       content-type
          :serval.response/character-encoding character-encoding)))

(defn write-body!
  "Writes a response body immediately.

  Context is unchanged."
  ([{:serval.context/keys [response] :as ctx} body]
   (response.body/write-response-body response body)
   ctx)
  ([{:serval.context/keys [response] :as ctx} body content-type]
   (response.body/write-response-body response body content-type)
   ctx)
  ([{:serval.context/keys [response] :as ctx} body content-type character-encoding]
   (response.body/write-response-body response body content-type character-encoding)
   ctx))

(defn response
  "Sets the response status, body, content-type, and character-encoding."
  ([ctx status]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status status))
  ([ctx status body]
   {:pre [(int? status)]}
   (assoc ctx
          :serval.response/status status
          :serval.response/body   body))
  ([ctx status body content-type]
   {:pre [(int? status)]}
   (assoc ctx
          :serval.response/status       status
          :serval.response/body         body
          :serval.response/content-type content-type))
  ([ctx status body content-type character-encoding]
   {:pre [(int? status)]}
   (assoc ctx
          :serval.response/status             status
          :serval.response/body               body
          :serval.response/content-type       content-type
          :serval.response/character-encoding character-encoding)))

(defn set-response!
  "Sets the response status, body, content-type, and character-encoding immediately.

  Context is unchanged."
  ([{:serval.context/keys [response] :as ctx} status]
   {:pre [(int? status)]}
   (response.http/set-status response status)
   ctx)
  ([{:serval.context/keys [response] :as ctx} status body]
   {:pre [(int? status)]}
   (response.http/set-status response status)
   (response.body/write-response-body response body)
   ctx)
  ([{:serval.context/keys [response] :as ctx} status body content-type]
   {:pre [(int? status)]}
   (response.http/set-status response status)
   (response.body/write-response-body response body content-type)
   ctx)
  ([{:serval.context/keys [response] :as ctx} status body content-type character-encoding]
   {:pre [(int? status)]}
   (response.http/set-status response status)
   (response.body/write-response-body response body content-type character-encoding)
   ctx))

(defn set-response-kv!
  "Sets the response immediately given key value pairs.

  Context is unchanged."
  [{:serval.context/keys [response] :as ctx} & {:as m}]
  (response.http/set-response response m)
  ctx)

(defn send-redirect
  [ctx location]
  (assoc ctx :serval.response/send-redirect location))

(defn send-redirect!
  [{:serval.context/keys [^HttpServletResponse response] :as ctx} location]
  (.sendRedirect response location)
  ctx)

(defn send-error
  "Sets send-error with code and message."
  ([ctx code]
   (assoc ctx :serval.response/send-error code))
  ([ctx code message]
   (assoc ctx :serval.response/send-error [code message])))

(defn send-error!
  "Sends error immediately.

  Context is unchanged."
  ([{:serval.context/keys [^HttpServletResponse response] :as ctx} code]
   (.sendError response code)
   ctx)
  ([{:serval.context/keys [^HttpServletResponse response] :as ctx} code message]
   (.sendError response code message)
   ctx))

(defn do-filter-chain
  "Sets do-filter-chain, which is processed when the filter completes.
  Since do-filter-chain is processed after filter completion,
  this only allows for filter pre-processing."
  ([ctx]
   (assoc ctx :serval.filter/do-filter-chain true))
  ([ctx request response]
   (assoc ctx :serval.filter/do-filter-chain [request response])))

(defn do-filter-chain!
  "Does the filter chain immediately. Allows for filter post-processing.

  Context is unchanged."
  ([{:serval.context/keys [request response ^FilterChain filter-chain] :as ctx}]
   (.doFilter filter-chain request response)
   ctx)
  ([{:serval.context/keys [^FilterChain filter-chain] :as ctx} request response]
   (.doFilter filter-chain request response)
   ctx))

(defn log
  "Logs the message and throwable using ServletContext.

  Context is unchanged."
  ([{:serval.context/keys [^ServletRequest request] :as ctx} msg]
   (.. request
       (getServletContext)
       (log msg))
   ctx)
  ([{:serval.context/keys [^ServletRequest request] :as ctx} msg throwable]
   (.. request
       (getServletContext)
       (log msg throwable))
   ctx))

;; Context Utils

;; https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest

(defn get-request-input-stream
  "Returns the request input-stream.

  As per servlet spec, after this is called, get-request-reader may not be called."
  {:tag ServletInputStream}
  [{:serval.context/keys [^ServletRequest request]}]
  (.getInputStream request))

(defn get-request-reader
  "Returns the request reader.

  As per servlet spec, after this is called, get-request-output-stream may not be called."
  {:tag java.io.BufferedReader}
  [{:serval.context/keys [^ServletRequest request]}]
  (.getReader request))

;; https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletresponse

(defn get-response-output-stream
  "Returns the response output-stream.

  As per servlet spec, after this is called, get-response-writer may not be called."
  {:tag ServletOutputStream}
  [{:serval.context/keys [^ServletResponse response]}]
  (.getOutputStream response))

(defn get-response-writer
  "Returns the response writer.

  As per servlet spec, after this is called, get-response-output-stream may not be called."
  {:tag java.io.PrintWriter}
  [{:serval.context/keys [^ServletResponse response]}]
  (.getWriter response))

(def writable-to-writer
  response.body/writable-to-writer)

(def writable-to-output-stream
  response.body/writable-to-output-stream)
