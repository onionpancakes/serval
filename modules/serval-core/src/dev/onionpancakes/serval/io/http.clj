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

(defn write-response!
  [init-ctx resp-ctx]
  (let [^HttpServletResponse out (or (:serval.service/response resp-ctx)
                                     (:serval.service/response init-ctx))]
    (if-let [content-type (:serval.response/content-type resp-ctx)]
      (.setContentType out content-type))
    (if-let [encoding (:serval.response/character-encoding resp-ctx)]
      (.setCharacterEncoding out encoding))
    (if-let [status (:serval.response/status resp-ctx)]
      (.setStatus out status))
    (if-let [headers (:serval.response/headers resp-ctx)]
      (doseq [[name values] headers
              value         values]
        (p/write-header value out name)))
    (if-let [body (:serval.response/body resp-ctx)]
      (p/write-body body out))))
