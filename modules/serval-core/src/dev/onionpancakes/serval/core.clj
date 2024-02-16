(ns dev.onionpancakes.serval.core
  (:require [dev.onionpancakes.serval.response.body :as resp.body]
            [dev.onionpancakes.serval.response.http :as resp.http])
  (:import [jakarta.servlet ServletInputStream ServletOutputStream]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse]))

(defn set-http
  [response & {:as http-opts}]
  (resp.http/set-http response http-opts))

(defn write-body
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
