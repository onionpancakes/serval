(ns dev.onionpancakes.serval.io.body
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
    (.write out ^bytes this))
  String
  (write [this ^ServletOutputStream out _]
    (.print out this))
  Throwable
  (write [this ^ServletOutputStream out ^String enc]
    (.printStackTrace this (java.io.PrintStream. out true enc)))
  java.io.File
  (write [this out _]
    (Files/copy (.toPath this) out))
  java.io.InputStream
  (write [this out _]
    (with-open [in this]
      (.transferTo in out)))
  java.net.URL
  (write [this out _]
    (with-open [in (.openStream this)]
      (.transferTo in out)))
  java.nio.file.Path
  (write [this out _]
    (Files/copy this out))
  clojure.core.Eduction
  (write [this out enc]
    (let [xf (map #(write % out enc))]
      (transduce xf (constantly nil) nil this)))
  clojure.lang.ISeq
  (write [this out enc]
    (doseq [i this]
      (write i out enc)))
  nil
  (write [_ _ _] nil))

;; ResponseBody

(defprotocol ResponseBody
  (async-body? [this])
  (service-body [this servlet request response]))

(extend-protocol ResponseBody
  CompletionStage
  (async-body? [this] true)
  (service-body [this _ _ ^ServletResponse response]
    (.thenApply this (reify Function
                       (apply [_ body]
                         (let [out (.getOutputStream response)
                               enc (.getCharacterEncoding response)]
                           (write body out enc))))))
  Object
  (async-body? [this] false)
  (service-body [this _ _ ^ServletResponse response]
    (-> (write this (.getOutputStream response) (.getCharacterEncoding response))
        (CompletableFuture/completedFuture)))
  nil
  (async-body? [this] false)
  (service-body [this _ _ ^ServletResponse response]
    (-> (write this (.getOutputStream response) (.getCharacterEncoding response))
        (CompletableFuture/completedFuture))))
