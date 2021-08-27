(ns dev.onionpancakes.serval.io.protocols
  (:import [jakarta.servlet ServletResponse]
           [jakarta.servlet.http HttpServletResponse]))

(defprotocol ResponseBody
  (write-body [this response]))

(extend-protocol ResponseBody
  ;; To extend, primitive array must be first.
  ;; Also can't refer to primitives directly.
  ;; https://clojure.atlassian.net/browse/CLJ-1381
  (Class/forName "[B")
  (write-body [this ^ServletResponse response]
    (.write (.getOutputStream response) ^bytes this))
  String
  (write-body [this ^ServletResponse response]
    (.write (.getWriter response) this))
  java.io.InputStream
  (write-body [this ^ServletResponse response]
    (try
      (.transferTo this (.getOutputStream response))
      (finally
        (.close this)))))

;; HTTP

(defprotocol ResponseHeader
  (write-header [this response name]))

(extend-protocol ResponseHeader
  String
  (write-header [this ^HttpServletResponse response name]
    (.addHeader response name this))
  Long
  (write-header [this ^HttpServletResponse response name]
    (.addIntHeader response name this))
  Integer
  (write-header [this ^HttpServletResponse response name]
    (.addIntHeader response name this)))
