(ns dev.onionpancakes.serval.response.body
  (:import [jakarta.servlet ServletResponse ServletOutputStream]
           [java.io PrintWriter]
           [java.nio.file Files]
           [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function]))

;; Protocols

(defprotocol WritableToWriter
  (value-write-to-writer [this writer]))

(defprotocol WritableToOutputStream
  (value-write-to-output-stream [this out]))

(defprotocol Body
  (body-write-to-response [this response]))

(defn write-writer-value
  [writer value]
  (value-write-to-writer value writer)
  writer)

(defn write-output-stream-value
  [out value]
  (value-write-to-output-stream value out)
  out)

(defn write-response-body
  ([^ServletResponse response body]
   (body-write-to-response body response)
   response)
  ([^ServletResponse response body content-type]
   (.setContentType response content-type)
   (body-write-to-response body response)
   response)
  ([^ServletResponse response body content-type character-encoding]
   (.setContentType response content-type)
   (.setCharacterEncoding response character-encoding)
   (body-write-to-response body response)
   response))

(defn writable-to-writer
  ([value]
   (writable-to-writer write-writer-value value))
  ([write-fn value]
   (reify WritableToWriter
     (value-write-to-writer [_ writer]
       (write-fn writer value))
     Body
     (body-write-to-response [_ response]
       (write-fn (.getWriter ^ServletResponse response) value)))))

(defn writable-to-output-stream
  ([value]
   (writable-to-output-stream write-output-stream-value value))
  ([write-fn value]
   (reify WritableToOutputStream
     (value-write-to-output-stream [_ out]
       (write-fn out value))
     Body
     (body-write-to-response [_ response]
       (write-fn (.getOutputStream ^ServletResponse response) value)))))

;; Impl

(extend-protocol WritableToWriter
  clojure.core.Eduction
  (value-write-to-writer [this writer]
    (reduce write-writer-value writer this)
    nil)
  clojure.lang.ISeq
  (value-write-to-writer [this writer]
    (doseq [v this]
      (value-write-to-writer v writer))
    nil)
  CharSequence
  (value-write-to-writer [this ^PrintWriter writer]
    (.append writer this)
    nil)
  nil
  (write-to-writer [_ _] nil))

(extend-protocol WritableToOutputStream
  (Class/forName "[B")
  (value-write-to-output-stream [this ^ServletOutputStream out]
    (.write out ^bytes this)
    nil)
  clojure.core.Eduction
  (value-write-to-output-stream [this writer]
    (reduce write-output-stream-value writer this)
    nil)
  clojure.lang.ISeq
  (value-write-to-output-stream [this writer]
    (doseq [v this]
      (value-write-to-output-stream v writer))
    nil)
  java.io.File
  (value-write-to-output-stream [this ^ServletOutputStream out]
    (Files/copy (.toPath this) out)
    nil)
  java.io.InputStream
  (value-write-to-output-stream [this ^ServletOutputStream out]
    (with-open [in this]
      (.transferTo in out))
    nil)
  java.net.URL
  (value-write-to-output-stream [this ^ServletOutputStream out]
    (with-open [in (.openStream this)]
      (.transferTo in out))
    nil)
  java.nio.file.Path
  (value-write-to-output-stream [this ^ServletOutputStream out]
    (Files/copy this out)
    nil)
  nil
  (value-write-to-output-stream [_ _] nil))

(extend-protocol Body
  (Class/forName "[B")
  (body-write-to-response [this ^ServletResponse response]
    (value-write-to-output-stream this (.getOutputStream response)))
  java.io.File
  (body-write-to-response [this ^ServletResponse response]
    (value-write-to-output-stream this (.getOutputStream response)))
  java.io.InputStream
  (body-write-to-response [this ^ServletResponse response]
    (value-write-to-output-stream this (.getOutputStream response)))
  java.net.URL
  (body-write-to-response [this ^ServletResponse response]
    (value-write-to-output-stream this (.getOutputStream response)))
  java.nio.file.Path
  (body-write-to-response [this ^ServletResponse response]
    (value-write-to-output-stream this (.getOutputStream response)))
  CharSequence
  (body-write-to-response [this ^ServletResponse response]
    (value-write-to-writer this (.getWriter response)))
  ;; Default write to writer
  Object
  (body-write-to-response [this ^ServletResponse response]
    (value-write-to-writer this (.getWriter response)))
  nil
  (body-write-to-response [_ _] nil))
