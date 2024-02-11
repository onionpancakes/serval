(ns dev.onionpancakes.serval.response.http
  (:require [dev.onionpancakes.serval.response.body
             :as response.body])
  (:import [java.util.function Function BiConsumer Supplier]
           [jakarta.servlet.http HttpServletRequest HttpServletResponse]))

;; Status

(defn set-status
  [^HttpServletResponse response status]
  (doto response
    (.setStatus status)))

;; Headers

(defprotocol HeaderValue
  (add-header-to-response [this response header-name])
  (set-header-to-response [this response header-name]))

(extend-protocol HeaderValue
  String
  (add-header-to-response [this ^HttpServletResponse response header-name]
    (.addHeader response header-name this))
  (set-header-to-response [this ^HttpServletResponse response header-name]
    (.setHeader response header-name this))
  java.util.Date
  (add-header-to-response [this ^HttpServletResponse response header-name]
    (.addDateHeader response header-name (.getTime this)))
  (set-header-to-response [this ^HttpServletResponse response header-name]
    (.setDateHeader response header-name (.getTime this)))
  java.time.Instant
  (add-header-to-response [this ^HttpServletResponse response header-name]
    (.addDateHeader response header-name (.toEpochMilli this)))
  (set-header-to-response [this ^HttpServletResponse response header-name]
    (.setDateHeader response header-name (.toEpochMilli this)))
  Object
  (add-header-to-response [this ^HttpServletResponse response header-name]
    (.addHeader response header-name (.toString this)))
  (set-header-to-response [this ^HttpServletResponse response header-name]
    (.setHeader response header-name (.toString this))))

(defn add-header-to-response-from-indexed
  [this response header-name]
  (loop [i 0 cnt (count this)]
    (when (< i cnt)
      (-> (nth this i)
          (add-header-to-response response header-name))
      (recur (inc i) cnt))))

(defn add-header-to-response-from-seqable
  [this response header-name]
  (doseq [value this]
    (add-header-to-response value response header-name)))

(extend clojure.lang.Indexed
  HeaderValue
  {:add-header-to-response add-header-to-response-from-indexed
   :set-header-to-response add-header-to-response-from-indexed})

(extend clojure.lang.Seqable
  HeaderValue
  {:add-header-to-response add-header-to-response-from-seqable
   :set-header-to-response add-header-to-response-from-seqable})

(defn set-header
  [response header-name value]
  (set-header-to-response value response header-name)
  response)

(defn set-headers
  [response headers]
  (reduce-kv set-header response headers))

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

(defn set-trailers
  [^HttpServletResponse response trailers]
  (doto response
    (.setTrailerFields (as-trailer-fields-supplier trailers))))

;; Error

(defn send-error
  [^HttpServletResponse response err]
  (cond
    (int? err)    (.sendError response err)
    (vector? err) (.sendError response (nth err 0) (nth err 1))))

;; Response

(defn set-response
  [^HttpServletResponse response m]
  ;; Status
  (when (contains? m :serval.response/status)
    (set-status response (:serval.response/status m)))
  ;; Headers
  (when (contains? m :serval.response/headers)
    (set-headers response (:serval.response/headers m)))
  ;; Trailers
  (when (contains? m :serval.response/trailers)
    (set-trailers response (:serval.response/trailers m)))
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
  (when (contains? m :serval.response/body)
    (response.body/write-response-body response (:serval.response/body m)))
  (when (contains? m :serval.response/send-error)
    (send-error response (:serval.response/send-error m)))
  response)
