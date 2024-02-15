(ns dev.onionpancakes.serval.core
  (:require [dev.onionpancakes.serval.response.body :as resp.body]
            [dev.onionpancakes.serval.response.http :as resp.http])
  (:import [jakarta.servlet Servlet Filter FilterChain
            ServletInputStream ServletOutputStream]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse]))

(def ^:dynamic ^Servlet *servlet* nil)
(def ^:dynamic ^HttpServletRequest *servlet-request* nil)
(def ^:dynamic ^HttpServletResponse *servlet-response* nil)
(def ^:dynamic ^Filter *filter* nil)
(def ^:dynamic ^FilterChain *filter-chain* nil)

(defn set-http-with
  [response & {:as http-opts}]
  (resp.http/set-http response http-opts))

(defn set-http
  [& {:as http-opts}]
  (set-http-with *servlet-response* http-opts))

(defn write-body-with
  ([response body]
   (resp.body/write-body response body))
  ([response body & {:as http-opts}]
   (resp.http/set-http response http-opts)
   (resp.body/write-body response body)))

(defn write-body
  ([body]
   (write-body-with *servlet-response* body))
  ([body & {:as http-opts}]
   (write-body-with *servlet-response* body http-opts)))

(defn send-error-with
  ([^HttpServletResponse response code]
   (.sendError response code))
  ([^HttpServletResponse response code & {message :message :as http-opts}]
   (resp.http/set-http-for-send response http-opts)
   (.sendError response code message)))

(defn send-error
  ([code]
   (send-error-with *servlet-response* code))
  ([code & {:as http-opts}]
   (send-error-with *servlet-response* code http-opts)))

(defn send-redirect-with
  ([^HttpServletResponse response location]
   (.sendRedirect response location))
  ([^HttpServletResponse response location & {:as http-opts}]
   (resp.http/set-http-for-send response http-opts)
   (.sendRedirect response location)))

(defn send-redirect
  ([location]
   (send-redirect-with *servlet-response* location))
  ([location & {:as http-opts}]
   (send-redirect-with *servlet-response* http-opts)))
