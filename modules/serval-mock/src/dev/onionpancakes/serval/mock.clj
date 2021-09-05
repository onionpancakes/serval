(ns dev.onionpancakes.serval.mock
  (:require [clojure.core.protocols :as p])
  (:import [java.util Collections]
           [jakarta.servlet ServletRequest ServletResponse
            ServletInputStream ServletOutputStream AsyncContext
            ReadListener WriteListener]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse
            Cookie]
           [java.io ByteArrayInputStream ByteArrayOutputStream
            StringReader BufferedReader
            Writer StringWriter PrintWriter]))

(defrecord MockAsyncContext [data]
  AsyncContext
  (complete [this]
    (swap! data assoc :completed? true)
    nil))

(defn mock-async-context []
  (MockAsyncContext. (atom {})))

;; Request

(defn mock-servlet-input-stream
  [^ServletRequest req ^java.io.InputStream is]
  (let [finished (atom nil)]
    (proxy [ServletInputStream] []
      (read
        ([]
         (let [ret (.read is)]
           (if (== ret -1) (reset! finished true))
           ret))
        ([b]
         (let [ret (.read is b)]
           (if (== ret -1) (reset! finished true))
           ret)))

      ;; Async simulation thats synchronous.
      (isReady [] true)
      (isFinished [] @finished)
      (setReadListener [^ReadListener rl]
        (try
          (if (.getAsyncContext req)
            (do
              (.onDataAvailable rl)
              (if @finished
                (.onAllDataRead rl)))
            (throw (IllegalStateException. "Async not started.")))
          (catch Exception e
            (.onError rl e)))))))

(defrecord MockHttpServletRequest [data body-bytes body-string]
  HttpServletRequest
  (getAsyncContext [this]
    (if-let [actx (:async-context @data)]
      actx
      (throw (IllegalStateException. "Async not started."))))
  (isAsyncStarted [this]
    (boolean (:async-context @data)))
  (isAsyncSupported [this]
    (:async-supported? @data true))
  (startAsync [this]
    (let [actx (:async-context @data)]
      (cond
        actx                     actx
        (.isAsyncSupported this) (->> (mock-async-context)
                                      (swap! data assoc :async-context)
                                      (:async-context))
        :else                    (throw (IllegalStateException. "Async not supported.")))))
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
    (let [{:keys [reader input-stream]} @data]
      (cond
        reader       (throw (IllegalStateException. "Reader already called."))
        input-stream input-stream
        :else        (->> (ByteArrayInputStream. body-bytes)
                          (mock-servlet-input-stream this)
                          (swap! data assoc :input-stream)
                          (:input-stream)))))
  (getReader [this]
    (let [{:keys [reader input-stream]} @data]
      (cond
        input-stream (throw (IllegalStateException. "Input stream already called."))
        reader       reader
        :else        (->> (StringReader. body-string)
                          (BufferedReader.)
                          (swap! data assoc :reader)
                          (:reader))))))

(defn mock-http-servlet-request-string
  [data body ^String encoding]
  (MockHttpServletRequest. (atom data) (.getBytes body encoding) body))

#_(defn mock-http-servlet-request-bytes-only
  [data body]
  (MockHttpServletRequest. (atom data) body ""))

;; Response

(defn mock-servlet-output-stream
  [^ServletRequest req ^java.io.OutputStream os]
  (let [finished (atom false)]
    (proxy [ServletOutputStream] []
      (write
        ([x]
         (if (bytes? x)
           (.write os ^bytes x)
           (.write os ^int x)))
        ([b off len]
         (.write os b off len)))

      ;; Ghetto async simulation thats actually synchronous.
      (isReady [] true)
      (setWriteListener [^WriteListener wl]
        (try
          (.onWritePossible wl)
          (catch Exception e
            (.onError wl e)))))))

(defrecord MockHttpServletResponse [data req output-stream writer]
  HttpServletResponse
  (getCharacterEncoding [this]
    ;; Defaults to iso-8859-1
    ;; https://jakarta.ee/specifications/servlet/5.0/apidocs/jakarta/servlet/servletresponse#getCharacterEncoding()
    (:character-encoding @data "ISO-8859-1"))
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
    (let [{wtr :writer out :output-stream} @data]
      (cond
        wtr   (throw (IllegalStateException. "Writer already called."))
        out   out
        :else (->> (mock-servlet-output-stream req output-stream)
                   (swap! data assoc :output-stream)
                   (:output-stream)))))
  (getWriter [this]
    (let [{wtr :writer out :output-stream} @data]
      (cond
        out   (throw (IllegalStateException. "Ouput stream already called."))
        wtr   wtr
        :else (->> (PrintWriter. writer)
                   (swap! data assoc :writer)
                   (:writer))))))

(defn mock-http-servlet-response
  [data req]
  (let [out    (ByteArrayOutputStream.)
        writer (StringWriter.)]
    (MockHttpServletResponse. (atom data) req out writer)))
