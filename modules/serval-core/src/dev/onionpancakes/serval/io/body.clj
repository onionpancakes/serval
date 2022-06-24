(ns dev.onionpancakes.serval.io.body
  (:import [jakarta.servlet ServletResponse]
           [java.nio.file Files]
           [java.util.concurrent CompletionStage]))

;; Response body protocol

(defprotocol ResponseBody
  (service-body [this servlet request response]))

(extend-protocol ResponseBody
  (Class/forName "[B")
  (service-body [this _ _ ^ServletResponse response]
    (.. response getOutputStream (write ^bytes this))
    nil)
  String
  (service-body [this _ _ ^ServletResponse response]
    (.. response getOutputStream (print this))
    nil)
  java.io.InputStream
  (service-body [this _ _ ^ServletResponse response]
    (try
      (.transferTo this (.getOutputStream response))
      (finally
        (.close this)))
    nil)
  java.nio.file.Path
  (service-body [this _ _ ^ServletResponse response]
    (Files/copy this (.getOutputStream response))
    nil)
  java.io.File
  (service-body [this _ _ ^ServletResponse response]
    (Files/copy (.toPath this) (.getOutputStream response))
    nil)
  nil
  (service-body [_ _ _ _] nil)

  ;; Seq
  clojure.lang.ISeq
  (service-body [this servlet request response]
    (doseq [i this]
      (service-body i servlet request response))
    nil)

  ;; Async
  CompletionStage
  (service-body [this servlet request response]
    (.thenAccept this (reify java.util.function.Consumer
                        (accept [_ body]
                          ;; Value should not be another CompletionStage.
                          (service-body body servlet request response))))))
