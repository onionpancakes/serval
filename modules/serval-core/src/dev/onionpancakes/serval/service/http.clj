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
  Object
  (add-header-value [this header-name ^HttpServletResponse response]
    (.addHeader response header-name (.toString this)))
  (set-header-value [this header-name ^HttpServletResponse response]
    (.setHeader response header-name (.toString this))))

(defn add-header-value-from-random-access
  [^java.util.List this header-name response]
  (loop [i 0 cnt (.size this)]
    (when (< i cnt)
      (add-header-value (.get this i) header-name response)
      (recur (inc i) cnt))))

(extend java.util.RandomAccess
  HeaderValue
  {:add-header-value add-header-value-from-random-access
   :set-header-value add-header-value-from-random-access})

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

;; Service response

(defn async-response?
  [m]
  (and (contains? m :serval.response/body)
       (service.body/async-body? (:serval.response/body m))))

(defn set-response
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
        (service.body/set-body servlet request response))))

(defn complete-response
  [^CompletionStage stage _ ^HttpServletRequest request ^HttpServletResponse response]
  (.whenComplete stage (reify BiConsumer
                         (accept [_ _ throwable]
                           (if throwable
                             (->> (.getMessage ^Throwable throwable)
                                  (.sendError response 500)))
                           (if (.isAsyncStarted request)
                             (.. request (getAsyncContext) (complete)))))))

(defn service-response
  [this servlet ^HttpServletRequest request response]
  (let [_ (if (and (async-response? this)
                   (not (.isAsyncStarted request)))
            (.startAsync request))
        c (set-response this servlet request response)]
    (if (instance? CompletionStage c)
      (complete-response c servlet request response))))
