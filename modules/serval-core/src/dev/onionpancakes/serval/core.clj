(ns dev.onionpancakes.serval.core
  (:require [dev.onionpancakes.serval.response :as srv.response])
  (:import [jakarta.servlet Servlet Filter FilterChain
            ServletInputStream ServletOutputStream]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse]))

(def ^:dynamic ^Servlet *servlet* nil)
(def ^:dynamic ^HttpServletRequest *servlet-request* nil)
(def ^:dynamic ^HttpServletResponse *servlet-response* nil)
(def ^:dynamic ^Filter *filter* nil)
(def ^:dynamic ^FilterChain *filter-chain* nil)

(defn respond*
  ([response a]
   (srv.response/respond response a))
  ([response a & more]
   (srv.response/respond response a)
   (reduce srv.response/respond response more)))

(defn respond
  ([])
  ([a]
   (srv.response/respond *servlet-response* a))
  ([a & more]
   (let [resp *servlet-response*]
     (srv.response/respond resp a)
     (reduce srv.response/respond resp more))))

(defn status
  [code]
  code)

(defn headers
  [headers]
  headers)

(def content-type
  srv.response/content-type)

(def charset
  srv.response/charset)

(def body
  srv.response/body)
