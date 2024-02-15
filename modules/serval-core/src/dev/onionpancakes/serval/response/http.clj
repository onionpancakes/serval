(ns dev.onionpancakes.serval.response.http
  (:import [jakarta.servlet.http HttpServletResponse]))

(defprotocol HeaderValue
  (add-header-to-response [this response header-name])
  (set-header-to-response [this response header-name]))

;; Set

(defn set-header
  [response k value]
  (set-header-to-response value response (name k))
  response)

(defn set-headers
  [response headers]
  (reduce-kv set-header response headers))

(defn set-response
  [^HttpServletResponse response m]
  ;; Status
  (when (contains? m :status)
    (.setStatus response (:status m)))
  ;; Headers
  (when (contains? m :headers)
    (set-headers response (:serval.response/headers m)))
  ;; Trailers
  (when (contains? m :trailers)
    (.setTrailers response (:trailers m)))
  ;; Cookies
  (when (contains? m :cookies)
    (doseq [cookie (:cookies m)]
      (.addCookie response cookie)))
  ;; ContentType
  (when (contains? m :content-type)
    (.setContentType response (:content-type m)))
  ;; CharacterEncoding
  (when (contains? m :character-encoding)
    (.setCharacterEncoding response (:character-encoding m)))
  response)

;; Impl

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
