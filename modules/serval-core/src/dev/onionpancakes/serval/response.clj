(ns dev.onionpancakes.serval.response
  (:import [jakarta.servlet ServletOutputStream]
           [jakarta.servlet.http HttpServletResponse]
           [java.io PrintWriter]
           [java.nio.file Files]))

(defprotocol HeaderValue
  (add-header-to-response [this response header-name])
  (set-header-to-response [this response header-name]))

(defprotocol BodyValue
  (write-body-to-response [this response]))

(defprotocol WritableToOutputStream
  (write-to-output-stream [this out]))

(defprotocol WritableToWriter
  (write-to-writer [this writer]))

(defprotocol ResponseValue
  (respond-with-value [this response]))

;; Headers

(defn set-response-header-kv
  [response k v]
  (set-header-to-response v response (name k))
  response)

(defn set-response-headers
  [response headers]
  (reduce-kv set-response-header-kv response headers))

(extend-protocol HeaderValue
  String
  (add-header-to-response [this ^HttpServletResponse response header-name]
    (.addHeader response header-name this))
  (set-header-to-response [this ^HttpServletResponse response header-name]
    (.setHeader response header-name this))
  java.util.Date
  (add-header-to-response [this ^HttpServletResponse response header-name]
    (.addDateHeader response header-name (.getTime this)))
  (set-header-to-response [this ^HttpServletResponse response header-name]
    (.setDateHeader response header-name (.getTime this)))
  java.time.Instant
  (add-header-to-response [this ^HttpServletResponse response header-name]
    (.addDateHeader response header-name (.toEpochMilli this)))
  (set-header-to-response [this ^HttpServletResponse response header-name]
    (.setDateHeader response header-name (.toEpochMilli this)))
  Object
  (add-header-to-response [this ^HttpServletResponse response header-name]
    (.addHeader response header-name (.toString this)))
  (set-header-to-response [this ^HttpServletResponse response header-name]
    (.setHeader response header-name (.toString this))))

(defn add-header-to-response-from-indexed
  [this response header-name]
  (loop [i 0 cnt (count this)]
    (when (< i cnt)
      (-> (nth this i)
          (add-header-to-response response header-name))
      (recur (inc i) cnt))))

(defn add-header-to-response-from-seqable
  [this response header-name]
  (doseq [value this]
    (add-header-to-response value response header-name)))

(extend clojure.lang.Indexed
  HeaderValue
  {:add-header-to-response add-header-to-response-from-indexed
   :set-header-to-response add-header-to-response-from-indexed})

(extend clojure.lang.Seqable
  HeaderValue
  {:add-header-to-response add-header-to-response-from-seqable
   :set-header-to-response add-header-to-response-from-seqable})

;; Writable

(defn write-writer-value
  [writer value]
  (write-to-writer value writer)
  writer)

(defn write-output-stream-value
  [out value]
  (write-to-output-stream value out)
  out)

(defn writable-to-writer
  ([value]
   (writable-to-writer value write-writer-value))
  ([value write-fn]
   (reify
     WritableToWriter
     (write-to-writer [_ writer]
       (write-fn writer value))
     ResponseValue
     (respond-with-value [_ response]
       (write-fn (.getWriter ^HttpServletResponse response) value))))
  ([value write-fn a]
   (reify
     WritableToWriter
     (write-to-writer [_ writer]
       (write-fn writer value a))
     ResponseValue
     (respond-with-value [_ response]
       (write-fn (.getWriter ^HttpServletResponse response) value a))))
  ([value write-fn a b]
   (reify
     WritableToWriter
     (write-to-writer [_ writer]
       (write-fn writer value a b))
     ResponseValue
     (respond-with-value [_ response]
       (write-fn (.getWriter ^HttpServletResponse response) value a b))))
  ([value write-fn a b & more]
   (reify
     WritableToWriter
     (write-to-writer [_ writer]
       (apply write-fn writer value a b more))
     ResponseValue
     (respond-with-value [_ response]
       (apply write-fn (.getWriter ^HttpServletResponse response) value a b more)))))

(defn writable-to-output-stream
  ([value]
   (writable-to-output-stream value write-output-stream-value))
  ([value write-fn]
   (reify
     WritableToOutputStream
     (write-to-output-stream [_ out]
       (write-fn out value))
     ResponseValue
     (respond-with-value [_ response]
       (write-fn (.getOutputStream ^HttpServletResponse response) value))))
  ([value write-fn a]
   (reify
     WritableToOutputStream
     (write-to-output-stream [_ out]
       (write-fn out value a))
     ResponseValue
     (respond-with-value [_ response]
       (write-fn (.getOutputStream ^HttpServletResponse response) value a))))
  ([value write-fn a b]
   (reify
     WritableToOutputStream
     (write-to-output-stream [_ out]
       (write-fn out value a b))
     ResponseValue
     (respond-with-value [_ response]
       (write-fn (.getOutputStream ^HttpServletResponse response) value a b))))
  ([value write-fn a b & more]
   (reify
     WritableToOutputStream
     (write-to-output-stream [_ out]
       (apply write-fn out value a b more))
     ResponseValue
     (respond-with-value [_ response]
       (apply write-fn (.getOutputStream ^HttpServletResponse response) value a b more)))))

(extend-protocol BodyValue
  (Class/forName "[B")
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  ;; Body OutputStream
  java.io.File
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  java.io.InputStream
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  java.net.URL
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  java.nio.file.Path
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  ;; Body Writer
  clojure.core.Eduction
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-writer this (.getWriter response)))
  clojure.lang.ISeq
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-writer this (.getWriter response)))
  CharSequence
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-writer this (.getWriter response)))
  Object
  (write-body-to-response [this ^HttpServletResponse response]
    (write-to-writer (str this) (.getWriter response)))
  nil
  (write-body-to-response [_ response] response))

(extend-protocol WritableToOutputStream
  (Class/forName "[B")
  (write-to-output-stream [this ^ServletOutputStream out]
    (.write out ^bytes this)
    nil)
  clojure.core.Eduction
  (write-to-output-stream [this writer]
    (reduce write-output-stream-value writer this)
    nil)
  clojure.lang.ISeq
  (write-to-output-stream [this writer]
    (doseq [v this]
      (write-to-output-stream v writer))
    nil)
  java.io.File
  (write-to-output-stream [this ^ServletOutputStream out]
    (Files/copy (.toPath this) out)
    nil)
  java.io.InputStream
  (write-to-output-stream [this ^ServletOutputStream out]
    (with-open [in this]
      (.transferTo in out))
    nil)
  java.net.URL
  (write-to-output-stream [this ^ServletOutputStream out]
    (with-open [in (.openStream this)]
      (.transferTo in out))
    nil)
  java.nio.file.Path
  (write-to-output-stream [this ^ServletOutputStream out]
    (Files/copy this out)
    nil)
  nil
  (value-write-to-output-stream [_ _] nil))

(extend-protocol WritableToWriter
  clojure.core.Eduction
  (write-to-writer [this writer]
    (reduce write-writer-value writer this)
    nil)
  clojure.lang.ISeq
  (write-to-writer [this writer]
    (doseq [v this]
      (write-to-writer v writer))
    nil)
  CharSequence
  (write-to-writer [this ^PrintWriter writer]
    (.append writer this)
    nil)
  nil
  (write-to-writer [_ _] nil))

;; Respond

(defn respond
  [response value]
  (respond-with-value value response)
  response)

(extend-protocol ResponseValue
  ;; Composite
  clojure.lang.IPersistentVector
  (respond-with-value [this response]
    (reduce respond response this))
  ;; Status
  Integer
  (respond-with-value [this ^HttpServletResponse response]
    (doto response
      (.setStatus this)))
  Long
  (respond-with-value [this ^HttpServletResponse response]
    (doto response
      (.setStatus this)))
  ;; Headers
  clojure.lang.IPersistentMap
  (respond-with-value [this ^HttpServletResponse response]
    (set-response-headers this))
  java.util.Locale
  (respond-with-value [this  ^HttpServletResponse response]
    (.setLocale response this))
  ;; Body
  Object
  (respond-with-value [this response]
    (write-body-to-response this response))
  nil
  (respond-with-value [_ response] response))

;; ContentType

(deftype ContentType [value]
  ResponseValue
  (respond-with-value [_ response]
    (.setContentType ^HttpServletResponse response value)))

(defn content-type
  [value]
  (ContentType. value))

;; Charset

(deftype Charset [value]
  ResponseValue
  (respond-with-value [_ response]
    (.setCharacterEncoding ^HttpServletResponse response value)))

(defn charset
  [value]
  (Charset. value))

;; Body

(deftype Body [value content-type charset]
  ResponseValue
  (respond-with-value [_ response]
    (if (some? content-type)
      (.setContentType ^HttpServletResponse response content-type))
    (if (some? charset)
      (.setCharacterEncoding ^HttpServletResponse response charset))
    (write-body-to-response value response)))

(defn body
  ([value]
   (Body. value nil nil))
  ([value content-type]
   (Body. value content-type nil))
  ([value content-type charset]
   (Body. value content-type charset)))
