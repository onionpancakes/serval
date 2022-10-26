(ns dev.onionpancakes.serval.io.body
  (:import [jakarta.servlet ServletResponse ServletOutputStream]
           [java.nio.file Files]
           [java.util.concurrent CompletionStage]))

;; Writable

(defprotocol Writable
  (write [this out]))

(extend-protocol Writable
  (Class/forName "[B")
  (write [this ^ServletOutputStream out]
    (.write out ^bytes this))
  String
  (write [this ^ServletOutputStream out]
    (.print out this))
  java.io.InputStream
  (write [this out]
    (try
      (.transferTo this out)
      (finally
        (.close this))))
  java.nio.file.Path
  (write [this out]
    (Files/copy this out))
  java.io.File
  (write [this out]
    (Files/copy (.toPath this) out))
  nil
  (write [_ _] nil)
  clojure.lang.ISeq
  (write [this out]
    (doseq [i this]
      (write i out))))

;; ResponseBody

(defprotocol ResponseBody
  (service-body [this servlet request response]))

(extend-protocol ResponseBody
  CompletionStage
  (service-body [this _ _ ^ServletResponse response]
    (.thenAccept this (reify java.util.function.Consumer
                        (accept [_ body]
                          (write body (.getOutputStream response))))))
  Object
  (service-body [this _ _ ^ServletResponse response]
    (write this (.getOutputStream response))
    nil)
  nil
  (service-body [this _ _ ^ServletResponse response]
    (write this (.getOutputStream response))
    nil))
