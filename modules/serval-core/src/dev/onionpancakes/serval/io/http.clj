(ns dev.onionpancakes.serval.io.http
  (:require [dev.onionpancakes.serval.io.body :as io.body])
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function]
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
    (.getAttribute request k)))

(deftype Headers [^HttpServletRequest request]
  clojure.lang.ILookup
  (valAt [this key]
    (.valAt this key nil))
  (valAt [this key not-found]
    (->> (.getHeaders request key)
         (enumeration-seq)
         (vec))))

(def parameter-map-xf
  (clojure.core/map (juxt key (comp vec val))))

(defn parameter-map
  [m]
  (into {} parameter-map-xf m))

(defn servlet-request-lookup
  [^HttpServletRequest request key not-found]
  (case key
    ;; Attributes
    :attributes      (Attributes. request)
    :attribute-names (->> (.getAttributeNames request)
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
    :scheme       (.getScheme request)
    :server-name  (.getServerName request)
    :server-port  (.getServerPort request)
    :path         (.getRequestURI request)
    :context-path (.getContextPath request)
    :servlet-path (.getServletPath request)
    :path-info    (.getPathInfo request)
    :query-string (.getQueryString request)
    :parameters   (parameter-map (.getParameterMap request))

    ;; HTTP
    :protocol (.getProtocol request)
    :method   (keyword (.getMethod request))

    ;; Headers
    :headers            (Headers. request)
    :header-names       (->> (.getHeaderNames request)
                             (enumeration-seq)
                             (vec))
    :content-length     (.getContentLengthLong request)
    :content-type       (.getContentType request)
    :character-encoding (.getCharacterEncoding request)
    :locales            (->> (.getLocales request)
                             (enumeration-seq)
                             (vec))
    :cookies            (vec (.getCookies request))

    ;; Body
    :reader       (.getReader request)
    :input-stream (.getInputStream request)

    ;; Multipart throws exceptions if servlet is not configured,
    ;; or if request is malformed.
    ;; Users should use method rather than ILookup to access parts.
    ;; :parts        (vec (.getParts request))

    ;; TODO: Trailers fields?

    not-found))

(defn servlet-request-proxy
  [^HttpServletRequest request]
  (proxy [HttpServletRequestWrapper clojure.lang.ILookup] [request]
    (valAt
      ([key]
       (servlet-request-lookup this key nil))
      ([key not-found]
       (servlet-request-lookup this key not-found)))))

;; Context

(defn context
  [servlet request response]
  {:serval.service/servlet  servlet
   :serval.service/request  (servlet-request-proxy request)
   :serval.service/response response})

;; Write

(defprotocol Response
  (async-response? [this ctx])
  (write-response [this ctx]))

(defprotocol ResponseHeader
  (write-header [this response name]))

(extend-protocol ResponseHeader
  String
  (write-header [this ^HttpServletResponse response name]
    (.addHeader response name this))
  Long
  (write-header [this ^HttpServletResponse response name]
    (.addIntHeader response name this))
  Integer
  (write-header [this ^HttpServletResponse response name]
    (.addIntHeader response name this)))

(defn write-response-map
  [m ctx]
  (let [^HttpServletResponse out (:serval.service/response ctx)]
    (if-let [content-type (:serval.response/content-type m)]
      (.setContentType out content-type))
    (if-let [encoding (:serval.response/character-encoding m)]
      (.setCharacterEncoding out encoding))
    (if-let [status (:serval.response/status m)]
      (.setStatus out status))
    (if-let [headers (:serval.response/headers m)]
      (doseq [[name values] headers
              value         values]
        (write-header value out name)))
    (if-let [body (:serval.response/body m)]
      ;; Body is last expression, so if it returns a CompletionStage,
      ;; so does this.
      (io.body/write-body body ctx))))

(extend-protocol Response
  java.util.Map
  (async-response? [this ctx]
    (-> (:serval.response/body this)
        (io.body/async-body? ctx)))
  (write-response [this ctx]
    (write-response-map this ctx))
  CompletionStage
  (async-response? [this _] true)
  (write-response [this ctx]
    (.thenCompose this (reify Function
                         (apply [this input]
                           (or (write-response input ctx)
                               (CompletableFuture/completedStage nil)))))))

;; Service fn

(defn service-fn
  [handler]
  (fn [servlet ^HttpServletRequest request ^HttpServletResponse response]
    (let [ctx       (context servlet request response)
          hresp     (handler ctx)
          async-ctx (cond
                      (.isAsyncStarted request)   (.getAsyncContext request)
                      (async-response? hresp ctx) (.startAsync request))
          
          ;; TODO: Async listener / timeout
          ^CompletionStage cstage (write-response hresp ctx)]
      (when async-ctx
        (-> (or cstage (CompletableFuture/completedStage nil))
            (.thenRun (fn [] (.complete async-ctx)))
            (.exceptionally (reify Function
                              (apply [_ input]
                                (let [msg (.getMessage ^Throwable input)]
                                  (.sendError response 500 msg)
                                  (.complete async-ctx))))))))))
