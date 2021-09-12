(ns dev.onionpancakes.serval.mock.http
  (:require [dev.onionpancakes.serval.mock.async :as async]
            [dev.onionpancakes.serval.mock.io :as io])
  (:import [jakarta.servlet.http
            HttpServletRequest HttpServletResponse Cookie]
           [java.util Collections]
           [java.io
            ByteArrayInputStream ByteArrayOutputStream
            BufferedReader InputStreamReader
            PrintWriter OutputStreamWriter]))

(defrecord MockHttpServletRequest [data ^java.io.InputStream input-stream]
  HttpServletRequest
  (getAsyncContext [this]
    (or (:async-context @data)
        (throw (IllegalStateException. "Async not started."))))
  (isAsyncStarted [this]
    (let [dval @data]
      (and (some? (:async-context dval))
           (not (:async-complete? @(:data (:async-context dval)))))))
  (isAsyncSupported [this]
    (boolean (:async-supported? @data true)))
  (startAsync [this]
    (let [dval @data
          actx (:async-context dval)]
      (or (and actx (not (:async-complete? @(:data actx))) actx)
          (if (:async-supported? @data true)
            (->> (async/async-context (atom {}))
                 (swap! data assoc :async-context)
                 (:async-context)))
          (throw (IllegalStateException. "Async not supported.")))))
  (getAttribute [this name]
    (get-in @data [:attributes name]))
  (getAttributeNames [this]
    (Collections/enumeration (or (keys (:attributes @data)) [])))
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
    (into {} (map (juxt key (comp #(into-array String %) val)))
          (:parameter-map @data)))
  (getProtocol [this]
    (:protocol @data))
  (getMethod [this]
    (:method @data))
  (getHeaders [this name]
    (Collections/enumeration (get-in @data [:headers name] [])))
  (getHeaderNames [this]
    (Collections/enumeration (or (keys (:headers @data)) [])))
  (getContentLength [this]
    (:content-length @data))
  (getContentLengthLong [this]
    (:content-length @data))
  (getContentType [this]
    (:content-type @data))
  (getCharacterEncoding [this]
    (:character-encoding @data))
  (getLocales [this]
    (Collections/enumeration (:locales @data [])))
  (getCookies [this]
    (into-array Cookie (:cookies @data)))
  (getInputStream [this]
    (let [dval @data]
      (if (:reader dval)
        (throw (IllegalStateException. "getReader already called.")))
      (or (:input-stream dval)
          (->> (io/servlet-input-stream (atom {}) this input-stream)
               (swap! data assoc :input-stream)
               (:input-stream)))))
  (getReader [this]
    (let [dval @data]
      (if (:input-stream dval)
        (throw (IllegalStateException. "getInputStream already called.")))
      (or (:reader dval)
          (->> ^String (:character-encoding dval "UTF-8")
               (InputStreamReader. input-stream)
               (BufferedReader.)
               (swap! data assoc :reader)
               (:reader))))))

(defrecord MockHttpServletResponse [data req ^java.io.OutputStream output-stream]
  HttpServletResponse
  (getCharacterEncoding [this]
    ;; Defaults to iso-8859-1
    ;; https://jakarta.ee/specifications/servlet/5.0/apidocs/jakarta/servlet/servletresponse#getCharacterEncoding()
    (:character-encoding @data "ISO-8859-1"))
  (sendError [this sc msg]
    (swap! data assoc :send-error [sc msg]))
  (setStatus [this sc]
    (swap! data assoc :status sc))
  (addHeader [this name value]
    (swap! data update-in [:headers name] (fnil conj []) value))
  (addIntHeader [this name value]
    (swap! data update-in [:headers name] (fnil conj []) value))
  (setContentType [this value]
    (swap! data assoc :content-type value))
  (setCharacterEncoding [this value]
    (swap! data assoc :character-encoding value))
  (getOutputStream [this]
    (let [dval @data]
      (if (:writer dval)
        (throw (IllegalStateException. "getWriter already called.")))
      (or (:output-stream dval)
          (->> (io/servlet-output-stream (atom nil) req output-stream)
               (swap! data assoc :output-stream)
               (:output-stream)))))
  (getWriter [this]
    (let [dval @data]
      (if (:output-stream dval)
        (throw (IllegalStateException. "getOutputStream already called.")))
      (or (:writer dval)
          (->> ^String (:character-encoding dval "ISO-8859-1")
               (OutputStreamWriter. output-stream)
               (PrintWriter.)
               (swap! data assoc :writer)
               (:writer))))))

;; API

(defn ^HttpServletRequest http-servlet-request
  ([data body]
   (http-servlet-request data body nil))
  ([data body opts]
   (cond
     (string? body) (->> (.getBytes ^String body ^String (:encoding opts "UTF-8"))
                         (ByteArrayInputStream.)
                         (MockHttpServletRequest. (atom data)))
     (bytes? body)  (->> (ByteArrayInputStream. body)
                         (MockHttpServletRequest. (atom data))))))

(defn ^HttpServletResponse http-servlet-response
  [data req]
  (let [out (ByteArrayOutputStream.)]
    (MockHttpServletResponse. (atom data) req out)))
