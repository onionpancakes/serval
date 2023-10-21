(ns dev.onionpancakes.serval.io.http
  (:require [dev.onionpancakes.serval.io.body :as io.body])
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function BiConsumer]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse]))

;; Headers

(defprotocol HeaderValue
  (add-header [this header-name response]))

(extend-protocol HeaderValue
  String
  (add-header [this header-name ^HttpServletResponse response]
    (.addHeader response header-name this))
  java.lang.Integer
  (add-header [this header-name ^HttpServletResponse response]
    (.addIntHeader response header-name this))
  java.util.Date
  (add-header [this header-name ^HttpServletResponse response]
    (.addDateHeader response header-name (.getTime this)))
  java.time.Instant
  (add-header [this header-name ^HttpServletResponse response]
    (.addDateHeader response header-name (.toEpochMilli this)))
  Object
  (add-header [this header-name ^HttpServletResponse response]
    (.addHeader response header-name (str this))))

(defn add-response-headers-rf
  [response header-name values]
  (loop [i 0]
    (when (< i (count values))
      (add-header (nth values i) header-name response)
      (recur (inc i))))
  response)

(defn add-response-headers
  [response headers]
  (reduce-kv add-response-headers-rf response headers))

;; Trailers

(defprotocol Trailers
  (as-trailer-fields-supplier [this]))

(extend-protocol Trailers
  clojure.lang.IDeref
  (as-trailer-fields-supplier [this]
    (reify java.util.function.Supplier
      (get [_]
        (deref this))))
  java.util.concurrent.Future
  (as-trailer-fields-supplier [this]
    (reify java.util.function.Supplier
      (get [_]
        (.get this))))
  java.util.Map
  (as-trailer-fields-supplier [this]
    (reify java.util.function.Supplier
      (get [_] this)))
  java.util.function.Supplier
  (as-trailer-fields-supplier [this] this))

;; HttpResponse

(defn service-response-from-map
  [m servlet request ^HttpServletResponse response]
  ;; Status
  (when (contains? m :serval.response/status)
    (.setStatus response (:serval.response/status m)))
  ;; Headers
  (when (contains? m :serval.response/headers)
    (add-response-headers response (:serval.response/headers m)))
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
  ;; Trailers
  (when (contains? m :serval.response/trailers)
    (->> (:serval.response/trailers m)
         (as-trailer-fields-supplier)
         (.setTrailerFields response)))
  ;; Body
  ;; Return CompletionStage from service-body.
  (when (contains? m :serval.response/body)
    (-> (:serval.response/body m)
        (io.body/service-body servlet request response))))

(defprotocol HttpResponse
  (async-response? [this])
  (service-response [this servlet request response]))

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
                           (let [cs (service-response input servlet request response)]
                             (if (instance? CompletionStage cs)
                               cs
                               (CompletableFuture/completedFuture cs))))))))

;; HttpResponseCompletable

(defprotocol HttpResponseCompletable
  (complete-response [this servlet request response]))

(extend-protocol HttpResponseCompletable
  CompletionStage
  (complete-response [this _ request response]
    (.whenComplete this (reify BiConsumer
                          (accept [_ _ throwable]
                            (if throwable
                              (->> (.getMessage ^Throwable throwable)
                                   (.sendError response 500)))
                            (if (.isAsyncStarted request)
                              (.. request (getAsyncContext) (complete)))))))
  Object
  (complete-response [_ _ request _]
    (if (.isAsyncStarted request)
      (.. request (getAsyncContext) (complete))))
  nil
  (complete-response [_ _ request _]
    (if (.isAsyncStarted request)
      (.. request (getAsyncContext) (complete)))))

;; Service

(defn service
  [this servlet ^HttpServletRequest request response]
  (let [_ (if (and (async-response? this)
                   (not (.isAsyncStarted request)))
            (.startAsync request))]
    (-> (service-response this servlet request response)
        (complete-response servlet request response))))
