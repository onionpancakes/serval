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

(defn set-response-with
  [response & {:as http-opts}]
  (resp.http/set-response response http-opts))

(defn set-response
  [& {:as http-opts}]
  (resp.http/set-response *servlet-response* http-opts))

(defn respond-with
  ([response body]
   (resp.body/write-body response body))
  ([response body & {:as http-opts}]
   (resp.http/set-response response http-opts)
   (resp.body/write-body response body)))

(defn respond
  ([body]
   (resp.body/write-body *servlet-response* body))
  ([body & {:as http-opts}]
   (let [response *servlet-response*]
     (resp.http/set-response response http-opts)
     (resp.body/write-body response body))))
