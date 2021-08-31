(ns dev.onionpancakes.serval.io.http
  (:require [dev.onionpancakes.serval.io :as io])
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

(deftype AttributeNames [^HttpServletRequest request]
  clojure.lang.Seqable
  (seq [this]
    (->> (.getAttributeNames request)
         (enumeration-seq))))

(deftype Headers [^HttpServletRequest request]
  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (->> (.getHeaders request k)
         (enumeration-seq)
         (vec))))

(deftype HeaderNames [^HttpServletRequest request]
  clojure.lang.Seqable
  (seq [this]
    (->> (.getHeaderNames request)
         (enumeration-seq))))

(def parameter-map-xf
  (clojure.core/map (juxt key (comp vec val))))

(defn parameter-map
  [m]
  (into {} parameter-map-xf m))

(defn servlet-request-proxy
  [^HttpServletRequest request]
  (proxy [HttpServletRequestWrapper clojure.lang.ILookup] [request]
    (valAt
      ([key]
       (.valAt ^clojure.lang.ILookup this key nil))
      ([key not-found]
       (case key
         ;; URL
         :method       (keyword (.getMethod request))
         :path         (.getRequestURI request)
         :context-path (.getContextPath request)
         :servlet-path (.getServletPath request)
         :path-info    (.getPathInfo request)
         :query-string (.getQueryString request)
         :parameters   (parameter-map (.getParameterMap request))
         
         ;; Attributes
         :attributes      (Attributes. request)
         :attribute-names (AttributeNames. request)

         ;; Headers
         :headers         (Headers. request)
         :header-names    (HeaderNames. request)
         
         ;; Body
         :reader             (.getReader request)
         :input-stream       (.getInputStream request)
         :content-length     (.getContentLengthLong request)
         :content-type       (.getContentType request)
         :character-encoding (.getCharacterEncoding request)
         
         not-found)))))

;; Context

(defn context
  [servlet request response]
  {:serval.service/servlet  servlet
   :serval.service/request  (servlet-request-proxy request)
   :serval.service/response response})

;; Write

(defprotocol Response
  (async-response? [this])
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
  (let [^HttpServletResponse out (or (:serval.service/response m)
                                     (:serval.service/response ctx))]
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
      (io/write-body body out))))

(extend-protocol Response
  java.util.Map
  (async-response? [this]
    (if-let [body (get this :serval.response/body)]
      (io/async-body? body)
      false))
  (write-response [this ctx]
    (write-response-map this ctx))
  CompletionStage
  (async-response? [this] true)
  (write-response [this ctx]
    (.thenCompose this (reify Function
                         (apply [this input]
                           (or (write-response input ctx)
                               (CompletableFuture/completedStage nil)))))))

;; Service fn

(defn service-fn
  [handler]
  (fn [servlet ^HttpServletRequest request ^HttpServletResponse response]
    (let [ctx                     (context servlet request response)
          hresp                   (handler ctx)
          async-ctx               (if (async-response? hresp)
                                    (.startAsync request))
          ;; TODO: Async listener / timeout
          ^CompletionStage cstage (write-response (handler ctx) ctx)]
      (when (and async-ctx (or cstage (CompletableFuture/completedStage nil)))
        (-> cstage
            (.thenRun (fn [] (.complete async-ctx)))
            (.exceptionally (reify Function
                              (apply [_ input]
                                (.sendError response 500 (.getMessage ^Throwable input))
                                (.complete async-ctx)))))))))
