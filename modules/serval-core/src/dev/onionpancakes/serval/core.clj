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

(defn set-http*
  [response & {:as http-opts}]
  (resp.http/set-http response http-opts))

(defn set-http
  [& {:as http-opts}]
  (resp.http/set-http *servlet-response* http-opts))

(defn write-body*
  ([response body]
   (resp.body/write-body response body))
  ([response body & more]
   (resp.body/write-body response body)
   (reduce write-body* response more)))

(defn write-body
  ([body]
   (resp.body/write-body *servlet-response* body))
  ([body & more]
   (let [response *servlet-response*]
     (resp.body/write-body response body)
     (reduce resp.body/write-body response more))))

(defn send-error*
  ([^HttpServletResponse response code]
   (.sendError response code))
  ([^HttpServletResponse response code message]
   (.sendError response code message)))

(defn send-error
  ([code]
   (.sendError *servlet-response* code))
  ([code message]
   (.sendError *servlet-response* code message)))

(defn send-redirect*
  ([^HttpServletResponse response location]
   (.sendRedirect response location)))

(defn send-redirect
  ([location]
   (.sendRedirect *servlet-response* location)))
