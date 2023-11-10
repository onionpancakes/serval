(ns dev.onionpancakes.serval.service.http
  (:require [dev.onionpancakes.serval.service.body
             :as service.body])
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function BiConsumer Supplier]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse]))

;; Headers

(defprotocol HeaderValue
  (add-header-value [this header-name response])
  (set-header-value [this header-name response]))

(extend-protocol HeaderValue
  String
  (add-header-value [this header-name ^HttpServletResponse response]
    (.addHeader response header-name this))
  (set-header-value [this header-name ^HttpServletResponse response]
    (.setHeader response header-name this))
  java.util.Date
  (add-header-value [this header-name ^HttpServletResponse response]
    (.addDateHeader response header-name (.getTime this)))
  (set-header-value [this header-name ^HttpServletResponse response]
    (.setDateHeader response header-name (.getTime this)))
  java.time.Instant
  (add-header-value [this header-name ^HttpServletResponse response]
    (.addDateHeader response header-name (.toEpochMilli this)))
  (set-header-value [this header-name ^HttpServletResponse response]
    (.setDateHeader response header-name (.toEpochMilli this)))
  ;; List
  java.util.RandomAccess
  (add-header-value [this header-name response]
    (loop [i 0 cnt (count this)]
      (when (< i cnt)
        (add-header-value (nth this i) header-name response)
        (recur (inc i) cnt))))
  (set-header-value [this header-name response]
    (add-header-value this header-name response))
  ;; Default
  Object
  (add-header-value [this header-name ^HttpServletResponse response]
    (.addHeader response header-name (str this)))
  (set-header-value [this header-name ^HttpServletResponse response]
    (.setHeader response header-name (str this))))

(defn set-response-header-values
  [response header-name values]
  (set-header-value values header-name response)
  response)

(defn set-response-headers
  [response headers]
  (reduce-kv set-response-header-values response headers))

;; Trailers

(defprotocol Trailers
  (as-trailer-fields-supplier [this]))

(extend-protocol Trailers
  clojure.lang.IDeref
  (as-trailer-fields-supplier [this]
    (reify Supplier
      (get [_]
        (deref this))))
  Supplier
  (as-trailer-fields-supplier [this] this))

;; HttpResponse

(defn service-response-from-map
  [m servlet request ^HttpServletResponse response]
  ;; Status
  (when (contains? m :serval.response/status)
    (.setStatus response (:serval.response/status m)))
  ;; Headers
  (when (contains? m :serval.response/headers)
    (set-response-headers response (:serval.response/headers m)))
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
        (service.body/service-body servlet request response))))

(defprotocol HttpResponse
  (async-response? [this])
  (service-response [this servlet request response]))

(extend-protocol HttpResponse
  java.util.Map
  (async-response? [this]
    (and (contains? this :serval.response/body)
         (service.body/async-body? (:serval.response/body this))))
  (service-response [this servlet request response]
    (service-response-from-map this servlet request response))
  CompletionStage
  (async-response? [this] true)
  (service-response [this servlet request response]
    (.thenCompose this (reify Function
                         (apply [_ m]
                           (let [ret (service-response-from-map m servlet request response)]
                             (if (instance? CompletionStage ret)
                               ret
                               (CompletableFuture/completedFuture ret))))))))

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
  (complete-response [_ _ _ _] nil)
  nil
  (complete-response [_ _ _ _] nil))

;; Service

(defn service
  [this servlet ^HttpServletRequest request response]
  (let [_ (if (and (async-response? this)
                   (not (.isAsyncStarted request)))
            (.startAsync request))]
    (-> (service-response this servlet request response)
        (complete-response servlet request response))))
