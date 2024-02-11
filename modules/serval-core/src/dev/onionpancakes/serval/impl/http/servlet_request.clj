(ns dev.onionpancakes.serval.impl.http.servlet-request
  (:import [jakarta.servlet.http
            HttpServletRequest
            HttpServletRequestWrapper]))

;; Attributes

(deftype AttributesProxy [^HttpServletRequest request]
  clojure.lang.ILookup
  (valAt [this k]
    (.get this k))
  (valAt [this k not-found]
    (if (.containsKey this k)
      (.get this k)
      not-found))
  java.util.Map
  (clear [this]
    (throw (UnsupportedOperationException.)))
  (containsKey [this k]
    (some? (.getAttribute request k)))
  (containsValue [this value]
    (->> (.getAttributeNames request)
         (enumeration-seq)
         (keep (comp #{value} #(.get this %)))
         (first)
         (some?)))
  (entrySet [this]
    (let [create-map-entry #(when-some [value (.get this %)]
                              (clojure.lang.MapEntry. % value))]
      (->> (.getAttributeNames request)
           (enumeration-seq)
           (into #{} (keep create-map-entry)))))
  (get [this k]
    (.getAttribute request k))
  (isEmpty [this]
    (->> (.getAttributeNames request)
         (enumeration-seq)
         (nil?)))
  (keySet [this]
    (set (enumeration-seq (.getAttributeNames request))))
  (put [this k value]
    (throw (UnsupportedOperationException.)))
  (putAll [this m]
    (throw (UnsupportedOperationException.)))
  (remove [this k]
    (throw (UnsupportedOperationException.)))
  (size [this]
    (count (enumeration-seq (.getAttributeNames request))))
  (values [this]
    (->> (.getAttributeNames request)
         (enumeration-seq)
         (into [] (keep #(.get this %))))))

(defn get-request-attributes
  [request]
  (AttributesProxy. request))

;; Parameters

(def get-request-parameters-xf
  (map (juxt key (comp vec val))))

(defn get-request-parameters
  [^HttpServletRequest request]
  (into {} get-request-parameters-xf (.getParameterMap request)))

;; Headers

(deftype HeadersProxy [^HttpServletRequest request]
  clojure.lang.ILookup
  (valAt [this k]
    (.get this k))
  (valAt [this k not-found]
    (if (.containsKey this k)
      (.get this k)
      not-found))
  java.util.Map
  (clear [this]
    (throw (UnsupportedOperationException.)))
  (containsKey [this k]
    (some? (.getHeader request k)))
  (containsValue [this value]
    (->> (.getHeaderNames request)
         (enumeration-seq)
         (keep (comp #{value} #(.get this %)))
         (first)
         (some?)))
  (entrySet [this]
    (let [create-map-entry #(when-some [value (.get this %)]
                              (clojure.lang.MapEntry. % value))]
      (->> (.getHeaderNames request)
           (enumeration-seq)
           (into #{} (keep create-map-entry)))))
  (get [this k]
    (->> (.getHeaders request k)
         (enumeration-seq)
         (vec)))
  (isEmpty [this]
    (->> (.getHeaderNames request)
         (enumeration-seq)
         (nil?)))
  (keySet [this]
    (set (enumeration-seq (.getHeaderNames request))))
  (put [this k value]
    (throw (UnsupportedOperationException.)))
  (putAll [this m]
    (throw (UnsupportedOperationException.)))
  (remove [this k]
    (throw (UnsupportedOperationException.)))
  (size [this]
    (count (enumeration-seq (.getHeaderNames request))))
  (values [this]
    (->> (.getHeaderNames request)
         (enumeration-seq)
         (into [] (keep #(.get this %))))))

(defn get-request-headers
  [request]
  (HeadersProxy. request))

;; Request

(def servlet-request-proxy-keys
  [;; Attributes
   :attributes
   ;; Request Id
   :request-id :protocol-request-id
   ;; Remote Connection
   :remote-addr :remote-host :remote-port
   ;; Local Connection
   :local-addr :local-name :local-port
   ;; DispatcherType
   :dispatcher-type
   ;; URI
   :scheme :server-name :server-port :path :context-path
   :servlet-path :path-info :query-string :parameters
   ;; HTTP
   :protocol :method
   ;; Headers
   :headers :content-length :content-type :character-encoding
   :cookies :locale :locales])

(defn servlet-request-proxy
  [^HttpServletRequest request]
  (proxy [HttpServletRequestWrapper clojure.lang.ILookup java.util.Map] [request]
    ;; clojure.lang.ILookup
    (valAt
      ([k]
       (.get ^java.util.Map this k))
      ([k not-found]
       (if (.containsKey ^java.util.Map this k)
         (.get ^java.util.Map this k)
         not-found)))
    ;; java.util.Map
    (clear []
      (throw (UnsupportedOperationException.)))
    (containsKey [k]
      (some? (.get ^java.util.Map this k)))
    (containsValue [value]
      (->> servlet-request-proxy-keys
           (keep (comp #{value} #(.get ^java.util.Map this %)))
           (first)
           (some?)))
    (entrySet []
      (let [create-map-entry #(when-some [value (.get ^java.util.Map this %)]
                                (clojure.lang.MapEntry. % value))]
        (into #{} (keep create-map-entry) servlet-request-proxy-keys)))
    (get [k]
      (case k
        ;; Attributes
        :attributes          (get-request-attributes request)
        ;; Request Id
        :request-id          (.getRequestId request)
        :protocol-request-id (.getProtocolRequestId request)
        ;; Remote connection
        :remote-addr         (.getRemoteAddr request)
        :remote-host         (.getRemoteHost request)
        :remote-port         (.getRemotePort request)
        ;; Local connection
        :local-addr          (.getLocalAddr request)
        :local-name          (.getLocalName request)
        :local-port          (.getLocalPort request)
        ;; DispatcherType
        :dispatcher-type     (.getDispatcherType request)
        ;; URI
        :scheme              (.getScheme request)
        :server-name         (.getServerName request)
        :server-port         (.getServerPort request)
        :path                (.getRequestURI request)
        :context-path        (.getContextPath request)
        :servlet-path        (.getServletPath request)
        :path-info           (.getPathInfo request)
        :query-string        (.getQueryString request)
        :parameters          (get-request-parameters request)
        ;; HTTP
        :protocol            (.getProtocol request)
        :method              (keyword (.getMethod request))
        ;; Headers
        :headers             (get-request-headers request)
        :content-length      (.getContentLengthLong request)
        :content-type        (.getContentType request)
        :character-encoding  (.getCharacterEncoding request)
        :cookies             (if-some [cookies (.getCookies request)]
                               (vec cookies))
        :locale              (.getLocale request)
        :locales             (enumeration-seq (.getLocales request))
        ;; Body
        ;; https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getInputStream()
        ;; Calling  getInputStream or getReader is a side-effect.
        ;; Use core helper methods or direct method calls instead.
        #_#_
        :body                (.getInputStream request)
        ;; Default nil
        nil))
    (isEmpty [] false)
    (keySet []
      (into #{} (filter #(.get ^java.util.Map this %)) servlet-request-proxy-keys))
    (put [k value]
      (throw (UnsupportedOperationException.)))
    (putAll [m]
      (throw (UnsupportedOperationException.)))
    (remove [k]
      (throw (UnsupportedOperationException.)))
    (size []
      (count (keep #(.get ^java.util.Map this %) servlet-request-proxy-keys)))
    (values []
      (into [] (keep #(.get ^java.util.Map this %)) servlet-request-proxy-keys))))
