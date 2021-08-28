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
