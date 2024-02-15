(ns dev.onionpancakes.serval.response.body
  (:import [jakarta.servlet ServletResponse ServletOutputStream]
           [java.io PrintWriter]
           [java.nio.file Files]))

;; Protocols

(defprotocol Body
  (write-body-to-response [this response]))

(defprotocol WritableToOutputStream
  (write-to-output-stream [this out]))

(defprotocol WritableToWriter
  (write-to-writer [this writer]))

;; Body

(defn write-body
  [response body]
  (write-body-to-response body response)
  response)

(extend-protocol Body
  (Class/forName "[B")
  (write-body-to-response [this ^ServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  ;; Body OutputStream
  java.io.File
  (write-body-to-response [this ^ServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  java.io.InputStream
  (write-body-to-response [this ^ServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  java.net.URL
  (write-body-to-response [this ^ServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  java.nio.file.Path
  (write-body-to-response [this ^ServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
  ;; Body Writer
  clojure.core.Eduction
  (write-body-to-response [this ^ServletResponse response]
    (write-to-writer this (.getWriter response)))
  clojure.lang.ISeq
  (write-body-to-response [this ^ServletResponse response]
    (write-to-writer this (.getWriter response)))
  CharSequence
  (write-body-to-response [this ^ServletResponse response]
    (write-to-writer this (.getWriter response)))
  Object
  (write-body-to-response [this ^ServletResponse response]
    (write-to-writer (str this) (.getWriter response)))
  nil
  (write-body-to-response [_ response] response))

;; OutputStream

(defn write-output-stream-value
  [out value]
  (write-to-output-stream value out)
  out)

(defn writable-to-output-stream
  ([value]
   (writable-to-output-stream value write-output-stream-value))
  ([value write-fn]
   (reify
     WritableToOutputStream
     (write-to-output-stream [_ out]
       (write-fn out value))
     Body
     (write-body-to-response [_ response]
       (write-fn (.getOutputStream ^ServletResponse response) value))))
  ([value write-fn a]
   (reify
     WritableToOutputStream
     (write-to-output-stream [_ out]
       (write-fn out value a))
     Body
     (write-body-to-response [_ response]
       (write-fn (.getOutputStream ^ServletResponse response) value a))))
  ([value write-fn a b]
   (reify
     WritableToOutputStream
     (write-to-output-stream [_ out]
       (write-fn out value a b))
     Body
     (write-body-to-response [_ response]
       (write-fn (.getOutputStream ^ServletResponse response) value a b))))
  ([value write-fn a b & more]
   (reify
     WritableToOutputStream
     (write-to-output-stream [_ out]
       (apply write-fn out value a b more))
     Body
     (write-body-to-response [_ response]
       (apply write-fn (.getOutputStream ^ServletResponse response) value a b more)))))

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

;; Writer

(defn write-writer-value
  [writer value]
  (write-to-writer value writer)
  writer)

(defn writable-to-writer
  ([value]
   (writable-to-writer value write-writer-value))
  ([value write-fn]
   (reify
     WritableToWriter
     (write-to-writer [_ writer]
       (write-fn writer value))
     Body
     (write-body-to-response [_ response]
       (write-fn (.getWriter ^ServletResponse response) value))))
  ([value write-fn a]
   (reify
     WritableToWriter
     (write-to-writer [_ writer]
       (write-fn writer value a))
     Body
     (write-body-to-response [_ response]
       (write-fn (.getWriter ^ServletResponse response) value a))))
  ([value write-fn a b]
   (reify
     WritableToWriter
     (write-to-writer [_ writer]
       (write-fn writer value a b))
     Body
     (write-body-to-response [_ response]
       (write-fn (.getWriter ^ServletResponse response) value a b))))
  ([value write-fn a b & more]
   (reify
     WritableToWriter
     (write-to-writer [_ writer]
       (apply write-fn writer value a b more))
     Body
     (write-body-to-response [_ response]
       (apply write-fn (.getWriter ^ServletResponse response) value a b more)))))

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
