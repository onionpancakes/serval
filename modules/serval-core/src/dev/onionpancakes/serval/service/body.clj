(ns dev.onionpancakes.serval.service.body
  (:import [jakarta.servlet ServletResponse ServletOutputStream]
           [java.nio.file Files]
           [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function]))

;; Writable

(defprotocol Writable
  (write [this out enc]))

(extend-protocol Writable
  (Class/forName "[B")
  (write [this ^ServletOutputStream out _]
    (.write out ^bytes this)
    nil)
  String
  (write [this ^ServletOutputStream out _]
    (.print out this)
    nil)
  Throwable
  (write [this ^ServletOutputStream out ^String enc]
    (.printStackTrace this (java.io.PrintStream. out true enc))
    nil)
  java.io.File
  (write [this out _]
    (Files/copy (.toPath this) out)
    nil)
  java.io.InputStream
  (write [this out _]
    (with-open [in this]
      (.transferTo in out))
    nil)
  java.net.URL
  (write [this out _]
    (with-open [in (.openStream this)]
      (.transferTo in out))
    nil)
  java.nio.file.Path
  (write [this out _]
    (Files/copy this out)
    nil)
  clojure.core.Eduction
  (write [this out enc]
    (let [rf (fn [_ v] (write v out enc) nil)]
      (reduce rf nil this))
    nil)
  clojure.lang.ISeq
  (write [this out enc]
    (doseq [i this]
      (write i out enc))
    nil)
  nil
  (write [_ _ _] nil))

;; Body

(defprotocol Body
  (async-body? [this])
  (set-body [this servlet request response]))

(extend-protocol Body
  CompletionStage
  (async-body? [this] true)
  (set-body [this _ _ ^ServletResponse response]
    (.thenApply this (reify Function
                       (apply [_ body]
                         (let [out (.getOutputStream response)
                               enc (.getCharacterEncoding response)]
                           (write body out enc))))))
  Object
  (async-body? [this] false)
  (set-body [this _ _ ^ServletResponse response]
    (write this (.getOutputStream response) (.getCharacterEncoding response)))
  nil
  (async-body? [this] false)
  (set-body [this _ _ ^ServletResponse response]
    (write this (.getOutputStream response) (.getCharacterEncoding response))))
