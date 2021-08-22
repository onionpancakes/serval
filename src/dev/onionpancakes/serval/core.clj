(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map])
  (:import [jakarta.servlet ServletOutputStream]
           [jakarta.servlet.http
            HttpServlet HttpServletRequest HttpServletResponse]))

(defprotocol IHeaderValue
  (write-header! [this k io]))

(extend-protocol IHeaderValue
  String
  (write-header! [this k io]
    (let [resp (:serval.service/response io)]
      (.addHeader ^HttpServletResponse resp k this)))
  Long
  (write-header! [this k io]
    (let [resp (:serval.service/response io)]
      (.addIntHeader ^HttpServletResponse resp k this)))
  Integer
  (write-header! [this k io]
    (let [resp (:serval.service/response io)]
      (.addIntHeader ^HttpServletResponse resp k this)))
  java.util.Collection
  (write-header! [this k io]
    (let [resp (:serval.service/response io)]
      (doseq [v this]
        (write-header! v k io)))))

(defn write-headers!
  [headers io]
  (doseq [[k v] headers]
    (write-header! v k io)))

(defprotocol IResponseBody
  (write-body! [this io]))

(extend-protocol IResponseBody
  ;; To extend, primitive array must be first.
  ;; Also can't refer to primitives directly.
  ;; https://clojure.atlassian.net/browse/CLJ-1381
  (Class/forName "[B")
  (write-body! [this io]
    (-> ^HttpServletResponse (:serval.service/response io)
        (.getOutputStream)
        (.write ^bytes this)))
  String
  (write-body! [this io]
    (-> ^HttpServletResponse (:serval.service/response io)
        (.getOutputStream)
        (.print this)))
  java.io.InputStream
  (write-body! [this io]
    (->> ^HttpServletResponse (:serval.service/response io)
         (.getOutputStream)
         (.transferTo this))))

(defprotocol IResponse
  (write-response! [this io]))

(extend-protocol IResponse
  java.util.Map
  (write-response! [this io]
    (let [^HttpServletResponse resp (:serval.service/response io)]
      (if-let [status (:serval.response/status this)]
        (.setStatus resp status))
      (if-let [headers (:serval.response/headers this)]
        (write-headers! headers io))
      (if-let [body (:serval.response/body this)]
        (write-body! body io)))))

;; Context

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

(defn context
  [servlet ^HttpServletRequest request response]
  {:serval.service/servlet  servlet
   :serval.service/request  request
   :serval.service/response response
   
   :serval.request/method          (keyword (.getMethod request))
   :serval.request/path            (.getRequestURI request)
   :serval.request/query-string    (.getQueryString request)
   :serval.request/parameters      (parameter-map (.getParameterMap request))

   :serval.request/attributes      (Attributes. request)
   :serval.request/attribute-names (AttributeNames. request)
   :serval.request/headers         (Headers. request)
   :serval.request/header-names    (HeaderNames. request)
   
   :serval.request/body               (.getInputStream request)
   :serval.request/content-length     (.getContentLengthLong request)
   :serval.request/content-type       (.getContentType request)
   :serval.request/character-encoding (.getCharacterEncoding request)})

;; Servlet

(defn service-fn
  [handler]
  (fn [servlet request response]
    (let [ctx (context servlet request response)]
      (-> (handler ctx)
          (write-response! ctx)))))

(defn servlet*
  [service-fn]
  (proxy [HttpServlet] []
    (service [request response]
      (service-fn this request response))))

(defn servlet
  [handler]
  (servlet* (service-fn handler)))

;; Middleware

(defn map
  [f]
  (fn [handler]
    (fn [input]
      (handler (f input)))))

(defn terminate
  [pred f]
  (fn [handler]
    (fn [input]
      (if (pred input)
        (f input)
        (handler input)))))

;; Handler

(def handler-xf
  (clojure.core/map #(% identity)))

(defn handler
  [& xfs]
  (->> (into '() handler-xf xfs)
       (apply comp)))
