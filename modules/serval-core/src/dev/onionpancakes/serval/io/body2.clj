(ns dev.onionpancakes.serval.io.body2
  (:import [jakarta.servlet ServletResponse]
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
    (.. response getWriter (write this))
    nil)
  java.io.InputStream
  (service-body [this _ _ ^ServletResponse response]
    (try
      (.transferTo this (.getOutputStream response))
      (finally
        (.close this)))
    nil)
  nil
  (service-body [_ _ _ _] nil)

  ;; Async
  CompletionStage
  (service-body [this servlet request response]
    (.thenAccept this (reify java.util.function.Consumer
                        (accept [_ body]
                          ;; Value should not be another CompletionStage.
                          (service-body body servlet request response))))))
