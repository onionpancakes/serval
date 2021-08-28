(ns dev.onionpancakes.serval.io.http
  (:require [dev.onionpancakes.serval.io.protocols :as p])
  (:import [jakarta.servlet.http
            HttpServlet HttpServletRequest HttpServletResponse
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
      (p/write-body body out))))

(extend-protocol Response
  java.util.Map
  (write-response [this ctx]
    (write-response-map this ctx)))
