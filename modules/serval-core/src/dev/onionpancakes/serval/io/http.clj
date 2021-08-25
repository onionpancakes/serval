(ns dev.onionpancakes.serval.io.http
  (:import [jakarta.servlet.http
            HttpServlet HttpServletRequest HttpServletResponse]))

;; Servlet

(deftype HttpServletProxy [^HttpServlet servlet]
  clojure.lang.ILookup
  (valAt [this key]
    (.vatAt this key nil))
  (valAt [this key not-found]
    (case key
      :instance servlet
      
      not-found)))

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

(deftype HttpServletRequestProxy [^HttpServletRequest request]
  clojure.lang.ILookup
  (valAt [this key]
    (.valAt this key nil))
  (valAt [this key not-found]
    (case key
      :instance request
      
      ;; URL
      :method       (keyword (.getMethod request))
      :path         (.getRequestURI request)
      :context-path (.getContextPath request)
      :servlet-path (.getServletPath request)
      :path-info    (.getPathInfo request)
      :query-string (.getQueryString request)
      :parameters   (parameter-map (.getParameterMap request))
      
      ;; Attributes / Headers
      :attributes      (Attributes. request)
      :attribute-names (AttributeNames. request)
      :headers         (Headers. request)
      :header-names    (HeaderNames. request)
      
      ;; Body
      :reader             (.getReader request)
      :input-stream       (.getInputStream request)
      :content-length     (.getContentLengthLong request)
      :content-type       (.getContentType request)
      :character-encoding (.getCharacterEncoding request)
      
      not-found)))

;; Response

(deftype HttpServletResponseProxy [^HttpServletResponse response]
  clojure.lang.ILookup
  (valAt [this key]
    (.valAt this key nil))
  (valAt [this key not-found]
    (case key
      :instance response

      :writer        (.getWriter response)
      :output-stream (.getOutputStream response)

      not-found)))

;; Read

(defn read-context
  [servlet ^HttpServletRequest request response]
  {:serval.service/servlet  (HttpServletProxy. servlet)
   :serval.service/request  (HttpServletRequestProxy. request)
   :serval.service/response (HttpServletResponseProxy. response)})

;; Write

(defprotocol IResponseHeader
  (write-header* [this resp k]))

(defprotocol IResponseBody
  (write-body* [this resp]))

(defn write-headers!
  [resp headers]
  (doseq [[k v] headers]
    (write-header* v resp k)))

(defn write-context!
  [servlet request ^HttpServletResponse response ctx]
  (if-let [status (:serval.response/status ctx)]
    (.setStatus response status))
  (if-let [headers (:serval.response/headers ctx)]
    (write-headers! response headers))
  (if-let [body (:serval.response/body ctx)]
    (write-body* body response)))

;; Impl

(extend-protocol IResponseHeader
  String
  (write-header* [this ^HttpServletResponse resp k]
    (.addHeader resp k this))
  Long
  (write-header* [this ^HttpServletResponse resp k]
    (.addIntHeader resp k this))
  Integer
  (write-header* [this ^HttpServletResponse resp k]
    (.addIntHeader resp k this))
  java.util.Collection
  (write-header* [this resp k]
    (doseq [v this]
      (write-header* v resp k))))

(extend-protocol IResponseBody
  ;; To extend, primitive array must be first.
  ;; Also can't refer to primitives directly.
  ;; https://clojure.atlassian.net/browse/CLJ-1381
  (Class/forName "[B")
  (write-body* [this ^HttpServletResponse resp]
    (.write (.getOutputStream resp) ^bytes this))
  String
  (write-body* [this ^HttpServletResponse resp]
    (.write (.getWriter resp) this))
  java.io.InputStream
  (write-body* [this ^HttpServletResponse resp]
    (try
      (.transferTo this (.getOutputStream resp))
      (finally
        (.close this)))))

