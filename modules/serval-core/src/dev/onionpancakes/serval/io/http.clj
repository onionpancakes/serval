(ns dev.onionpancakes.serval.io.http
  (:require [dev.onionpancakes.serval.io.body :as io.body])
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function BiConsumer]
           [jakarta.servlet.http
            HttpServletRequest
            HttpServletResponse
            HttpServletRequestWrapper]))

;; Request

(deftype Attributes [^HttpServletRequest request]
  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    ;; Can't use 'or' function because ret might be false.
    (if-some [val (.getAttribute request k)]
      val
      not-found)))

(deftype Headers [^HttpServletRequest request]
  clojure.lang.ILookup
  (valAt [this key]
    (.valAt this key nil))
  (valAt [this key not-found]
    (if-some [val (some->> (.getHeaders request key)
                           (enumeration-seq)
                           (vec))]
      val
      not-found)))

(defn lookup-servlet-request
  [^HttpServletRequest request k not-found]
  (case k
    ;; Attributes
    :attributes      (Attributes. request)
    :attribute-names (some->> (.getAttributeNames request)
                              (enumeration-seq)
                              (vec))

    :remote-addr (.getRemoteAddr request)
    :remote-host (.getRemoteHost request)
    :remote-port (.getRemotePort request)

    :local-addr (.getLocalAddr request)
    :local-name (.getLocalName request)
    :local-port (.getLocalPort request)

    :dispatcher-type (.getDispatcherType request)

    ;; URL
    :scheme        (.getScheme request)
    :server-name   (.getServerName request)
    :server-port   (.getServerPort request)
    :path          (.getRequestURI request)
    :context-path  (.getContextPath request)
    :servlet-path  (.getServletPath request)
    :path-info     (.getPathInfo request)
    :query-string  (.getQueryString request)
    :parameter-map (->> (.getParameterMap request)
                        (into {} (map (juxt key (comp vec val)))))

    ;; HTTP
    :protocol (.getProtocol request)
    :method   (keyword (.getMethod request))

    ;; Headers
    :headers            (Headers. request)
    :header-names       (some->> (.getHeaderNames request)
                                 (enumeration-seq)
                                 (vec))
    :content-length     (.getContentLengthLong request)
    :content-type       (.getContentType request)
    :character-encoding (.getCharacterEncoding request)
    :locales            (some->> (.getLocales request)
                                 (enumeration-seq)
                                 (vec))
    
    :cookies            (some->> (.getCookies request)
                                 (seq)
                                 (vec))

    ;; Body
    :reader       (.getReader request)
    :input-stream (.getInputStream request)

    ;; Multipart throws exceptions if servlet is not configured,
    ;; or if request is malformed.
    ;; Users should use method rather than ILookup to access parts.
    ;; :parts        (vec (.getParts request))

    ;; TODO: Trailers fields?

    not-found))

(defn servlet-request-lookup-proxy
  [^HttpServletRequest request]
  (proxy [HttpServletRequestWrapper clojure.lang.ILookup] [request]
    (valAt
      ([k]
       (lookup-servlet-request this k nil))
      ([k not-found]
       (lookup-servlet-request this k not-found)))))

;; Response

(defn service-response-map
  [m servlet request ^HttpServletResponse response]
  ;; Note: If content-type is not set,
  ;; character-encoding does not show up in headers.
  ;; TODO: warn if this is the case?
  (some->> (:serval.response/content-type m)
           (.setContentType response))
  (some->> (:serval.response/character-encoding m)
           (.setCharacterEncoding response))
  (doseq [cookie (:serval.response/cookies m)]
    (.addCookie response cookie))
  ;; Status / Headers / Body
  (some->> (:serval.response/status m)
           (.setStatus response))
  (doseq [[hname values] (:serval.response/headers m)
          value          values]
    ;; TODO: Add spec guard for header names, must be strings.
    (.addHeader response hname (str value)))
  ;; Notice: service-body may return a CompletionStage
  (some-> (:serval.response/body m)
          (io.body/service-body servlet request response)))

(defprotocol HttpResponse
  (service-response ^CompletionStage [this servlet request response]))

(extend-protocol HttpResponse
  java.util.Map
  (service-response [this servlet request response]
    (service-response-map this servlet request response))
  CompletionStage
  (service-response [this servlet request response]
    (.thenCompose this (reify Function
                         (apply [_ input]
                           (or (service-response input servlet request response)
                               (CompletableFuture/completedStage nil)))))))

;; Service

(defn context
  [servlet request response]
  {:serval.service/servlet  servlet
   :serval.service/request  (servlet-request-lookup-proxy request)
   :serval.service/response response})

(defn complete-async
  [^CompletionStage cstage ^HttpServletRequest request ^HttpServletResponse response]
  (when-let [atx (cond
                   (.isAsyncStarted request) (.getAsyncContext request)
                   (some? cstage)            (.startAsync request))]
    ;; TODO: handle if async is not supported?
    (if cstage
      (.whenComplete cstage (reify BiConsumer
                              (accept [_ _ throwable]
                                (if throwable
                                  (->> (.getMessage ^Throwable throwable)
                                       (.sendError response 500)))
                                (.complete atx))))
      (.complete atx))))

(defn service-fn
  [handler]
  (fn [servlet request response]
    (-> (context servlet request response)
        (handler)
        (service-response servlet request response)
        (complete-async request response))))
