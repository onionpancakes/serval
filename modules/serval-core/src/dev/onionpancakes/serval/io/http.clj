(ns dev.onionpancakes.serval.io.http
  (:require [dev.onionpancakes.serval.io.body :as io.body])
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [jakarta.servlet.http HttpServletResponse]))

(defn service-response-from-map
  [m servlet request ^HttpServletResponse response]
  ;; Status
  (when (contains? m :serval.response/status)
    (.setStatus response (:serval.response/status m)))
  ;; Headers
  (when (contains? m :serval.response/headers)
    (doseq [[header-name values] (:serval.response/headers m)
            value                values]
      (.addHeader response header-name (str value))))
  ;; Cookies
  (when (contains? m :serval.response/cookies)
    (doseq [cookie (:serval.response/cookies m)]
      (.addCookie response cookie)))
  ;; ContentType
  ;; Note: If content-type is not set,
  ;; character-encoding does not show up in headers.
  ;; TODO: warn if this is the case?
  (when (contains? m :serval.response/content-type)
    (.setContentType response (:serval.response/content-type m)))
  ;; CharacterEncoding
  (when (contains? m :serval.response/character-encoding)
    (.setCharacterEncoding response (:serval.response/character-encoding m)))
  ;; Body
  ;; Return CompletionStage from service-body.
  (if (contains? m :serval.response/body)
    (-> (:serval.response/body m)
        (io.body/service-body servlet request response))
    (CompletableFuture/completedFuture nil)))

(defprotocol HttpResponse
  (async-response? [this])
  (^CompletionStage service-response [this servlet request response]))

(extend-protocol HttpResponse
  java.util.Map
  (async-response? [this]
    (and (contains? this :serval.response/body)
         (io.body/async-body? (:serval.response/body this))))
  (service-response [this servlet request response]
    (service-response-from-map this servlet request response))
  CompletionStage
  (async-response? [this] true)
  (service-response [this servlet request response]
    (.thenCompose this (reify java.util.function.Function
                         (apply [_ input]
                           (service-response input servlet request response))))))
