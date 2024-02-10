(ns dev.onionpancakes.serval.response.body
  (:import [jakarta.servlet ServletResponse ServletOutputStream]
           [java.io PrintWriter]
           [java.nio.file Files]
           [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function]))

;; Protocols

(defprotocol WritableToWriter
  (write-to-writer [this writer]))

(defprotocol WritableToOutputStream
  (write-to-output-stream [this out]))

(defprotocol Body
  (write-body-to-response [this response]))

(defn write-body
  ([^ServletResponse response body]
   (write-body-to-response body response)
   response)
  ([^ServletResponse response body content-type]
   (.setContentType response content-type)
   (write-body-to-response body response)
   response)
  ([^ServletResponse response body content-type character-encoding]
   (.setContentType response content-type)
   (.setCharacterEncoding response character-encoding)
   (write-body-to-response body response)
   response))

;; Impl

(deftype BodyToOutputStream [value]
  Body
  (write-body-to-response [this response]
    (write-to-output-stream value (.getOutputStream ^ServletResponse response))))

(defn body-to-output-stream
  [value]
  (BodyToOutputStream. value))

(extend-protocol WritableToWriter
  clojure.core.Eduction
  (write-to-writer [this writer]
    (let [rf (fn [_ v] (write-to-writer v writer) nil)]
      (reduce rf nil this))
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

(extend-protocol WritableToOutputStream
  (Class/forName "[B")
  (write-to-output-stream [this ^ServletOutputStream out]
    (.write out ^bytes this)
    nil)
  clojure.core.Eduction
  (write-to-writer [this writer]
    (let [rf (fn [_ v] (write-to-output-stream v writer) nil)]
      (reduce rf nil this))
    nil)
  clojure.lang.ISeq
  (write-to-writer [this writer]
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
  (write-to-output-stream [_ _] nil))

(extend-protocol Body
  (Class/forName "[B")
  (write-body-to-response [this ^ServletResponse response]
    (write-to-output-stream this (.getOutputStream response)))
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
  CharSequence
  (write-body-to-response [this ^ServletResponse response]
    (write-to-writer this (.getWriter response)))
  ;; Default write to writer
  Object
  (write-body-to-response [this ^ServletResponse response]
    (write-to-writer this (.getWriter response)))
  nil
  (write-body-to-response [_ _] nil))
