(ns dev.onionpancakes.serval.impl.http.servlet-request
  (:import [jakarta.servlet.http
            HttpServletRequest
            HttpServletRequestWrapper]))

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
   :cookies :locale :locales
   ;; Body
   :body])

(defn servlet-request-proxy
  [^HttpServletRequest request]
  (proxy [HttpServletRequestWrapper java.util.Map] [request]
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
        ;; Attributes TODO
        :attributes          nil
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
        :parameters          nil
        ;; HTTP
        :protocol            (.getProtocol request)
        :method              (keyword (.getMethod request))
        ;; Headers TODO
        :headers             nil
        :content-length      (.getContentLengthLong request)
        :content-type        (.getContentType request)
        :character-encoding  (.getCharacterEncoding request)
        :cookies             (if-some [cookies (.getCookies request)]
                               (vec cookies))
        :locale              (.getLocale request)
        :locales             (enumeration-seq (.getLocales request))
        ;; Body
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
