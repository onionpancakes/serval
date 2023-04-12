(ns dev.onionpancakes.serval.io.body
  (:import [jakarta.servlet ServletResponse ServletOutputStream]
           [java.nio.file Files]
           [java.util.concurrent CompletionStage]))

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
  clojure.lang.ISeq
  (write [this out enc]
    (doseq [i this]
      (write i out enc)))
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
  nil
  (write [_ _ _] nil))

;; ResponseBody

(defprotocol ResponseBody
  (service-body [this servlet request response]))

(extend-protocol ResponseBody
  CompletionStage
  (service-body [this _ _ ^ServletResponse response]
    (.thenAccept this (reify java.util.function.Consumer
                        (accept [_ body]
                          (write body (.getOutputStream response) (.getCharacterEncoding response))))))
  Object
  (service-body [this _ _ ^ServletResponse response]
    (write this (.getOutputStream response) (.getCharacterEncoding response))
    nil)
  nil
  (service-body [this _ _ ^ServletResponse response]
    (write this (.getOutputStream response) (.getCharacterEncoding response))
    nil))
