(ns dev.onionpancakes.serval.mock.http
  (:require [dev.onionpancakes.serval.mock.async :as async]
            [dev.onionpancakes.serval.mock.io :as io])
  (:import [jakarta.servlet.http
            HttpServletRequest HttpServletResponse
            Cookie]
           [java.util Collections]
           [java.io ByteArrayInputStream ByteArrayOutputStream
            BufferedReader InputStreamReader
            Writer StringWriter PrintWriter]))

(defrecord MockHttpServletRequest [data body]
  HttpServletRequest
  (getAsyncContext [this]
    (or (:async-context @data)
        (throw (IllegalStateException. "Async not started."))))
  (isAsyncStarted [this]
    (boolean (:async-context @data)))
  (isAsyncSupported [this]
    (boolean (:async-supported? @data true)))
  (startAsync [this]
    (or (:async-context @data)
        (if (:async-supported? @data true)
          (->> (async/async-context (atom {}))
               (swap! data assoc :async-context)
               (:async-context)))
        (throw (IllegalStateException. "Async not supported."))))
  (getAttribute [this name]
    (get-in @data [:attributes name]))
  (getAttributeNames [this]
    (Collections/enumeration (keys (:attributes @data))))
  (getRemoteAddr [this]
    (:remote-addr @data))
  (getRemoteHost [this]
    (:remote-host @data))
  (getRemotePort [this]
    (:remote-port @data))
  (getLocalAddr [this]
    (:local-addr @data))
  (getLocalName [this]
    (:local-name @data))
  (getLocalPort [this]
    (:local-port @data))
  (getDispatcherType [this]
    (:dispatcher-type @data))
  (getScheme [this]
    (:scheme @data))
  (getServerName [this]
    (:server-name @data))
  (getServerPort [this]
    (:server-port @data))
  (getRequestURI [this]
    (:path @data))
  (getContextPath [this]
    (:context-path @data))
  (getServletPath [this]
    (:servlet-path @data))
  (getPathInfo [this]
    (:path-info @data))
  (getQueryString [this]
    (:query-string @data))
  (getParameterMap [this]
    (:parameter-map @data))
  (getProtocol [this]
    (:protocol @data))
  (getMethod [this]
    (:method @data))
  (getHeaders [this name]
    (Collections/enumeration (get-in @data [:headers name])))
  (getHeaderNames [this]
    (Collections/enumeration (keys (:headers @data))))
  (getContentLength [this]
    (:content-length @data))
  (getContentLengthLong [this]
    (:content-length @data))
  (getContentType [this]
    (:content-type @data))
  (getCharacterEncoding [this]
    (:character-encoding @data))
  (getLocales [this]
    (:locales @data))
  (getCookies [this]
    (into-array Cookie (:cookies @data)))
  (getInputStream [this]
    (if (:reader @data)
      (throw (IllegalStateException. "getReader already called.")))
    (or (:input-stream @data)
        (->> (ByteArrayInputStream. body)
             (io/servlet-input-stream (atom {}) this)
             (swap! data assoc :input-stream)
             (:input-stream))))
  (getReader [this]
    (if (:input-stream @data)
      (throw (IllegalStateException. "getInputStream already called.")))
    (or (:reader @data)
        (let [enc (:character-encoding @data "UTF-8")]
          (as-> (ByteArrayInputStream. body) x
            (InputStreamReader. x enc)
            (BufferedReader. x)
            (swap! data assoc :reader x)
            (:reader x))))))

(defrecord MockHttpServletResponse [data req]
  HttpServletResponse
  )

;; API

(defn http-servlet-request
  ([data body]
   (http-servlet-request data body nil))
  ([data body opts]
   (if (string? body)
     (MockHttpServletRequest. data (.getBytes body (:encoding opts "UTF-8")))
     (MockHttpServletRequest. data body))))

#_(defn http-servlet-response
  [data]
  (MockHttpServletResponse. data nil))
