(ns dev.onionpancakes.serval.core
  (:require [dev.onionpancakes.serval.response.body :as resp.body]
            [dev.onionpancakes.serval.response.http :as resp.http])
  (:import [jakarta.servlet FilterChain ServletRequest ServletResponse
            ServletInputStream ServletOutputStream]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse]))

(defn set-http
  {:tag HttpServletResponse}
  [response & {:as http-opts}]
  (resp.http/set-http response http-opts))

(defn write-body
  {:tag HttpServletResponse}
  ([response] response)
  ([response body]
   (resp.body/write-body response body))
  ([response body & more]
   (resp.body/write-body response body)
   (reduce resp.body/write-body response more)))

(defn send-error
  ([^HttpServletResponse response code]
   (.sendError response code))
  ([^HttpServletResponse response code message]
   (.sendError response code message)))

(defn send-redirect
  ([^HttpServletResponse response location]
   (.sendRedirect response location)))

(defn do-filter
  [^FilterChain chain request response]
  (.doFilter chain request response))

(defn get-input-stream
  {:tag ServletInputStream}
  [^ServletRequest request]
  (.getInputStream request))

(defn get-reader
  {:tag java.io.BufferedReader}
  [^ServletRequest request]
  (.getReader request))

(defn get-output-stream
  {:tag ServletOutputStream}
  [^ServletResponse response]
  (.getOutputStream response))

(defn get-writer
  {:tag java.io.PrintWriter}
  [^ServletResponse response]
  (.getWriter response))

(def writable-to-output-stream
  resp.body/writable-to-output-stream)

(def writable-to-writer
  resp.body/writable-to-writer)
