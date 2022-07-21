(ns dev.onionpancakes.serval.io.body
  (:import [jakarta.servlet ServletResponse]
           [java.nio.file Files]
           [java.util.concurrent CompletionStage]))

;; Sync

(defprotocol ResponseBodySync
  (service-body-sync [this servlet request response]))

(extend-protocol ResponseBodySync
  (Class/forName "[B")
  (service-body-sync [this _ _ ^ServletResponse response]
    (.. response getOutputStream (write ^bytes this)))
  String
  (service-body-sync [this _ _ ^ServletResponse response]
    (.. response getOutputStream (print this)))
  java.io.InputStream
  (service-body-sync [this _ _ ^ServletResponse response]
    (try
      (.transferTo this (.getOutputStream response))
      (finally
        (.close this))))
  java.nio.file.Path
  (service-body-sync [this _ _ ^ServletResponse response]
    (Files/copy this (.getOutputStream response)))
  java.io.File
  (service-body-sync [this _ _ ^ServletResponse response]
    (Files/copy (.toPath this) (.getOutputStream response)))
  nil
  (service-body-sync [_ _ _ _] nil)

  ;; Seq
  clojure.lang.ISeq
  (service-body-sync [this servlet request response]
    (doseq [i this]
      (service-body-sync i servlet request response))))

;; Response body protocol

(defprotocol ResponseBody
  (service-body [this servlet request response]))

(extend-protocol ResponseBody
  CompletionStage
  (service-body [this servlet request response]
    (.thenAccept this (reify java.util.function.Consumer
                        (accept [_ body]
                          (service-body body servlet request response)))))
  Object
  (service-body [this servlet request response]
    (service-body-sync this servlet request response)
    nil)
  nil
  (service-body [this servlet request response]
    (service-body-sync this servlet request response)
    nil))
