(ns dev.onionpancakes.serval.core
  (:refer-clojure :exclude [map])
  (:import [jakarta.servlet ServletOutputStream]
           [jakarta.servlet.http
            HttpServlet HttpServletRequest HttpServletResponse]))

(defprotocol IHeaderValue
  (write-header! [this k io]))

(extend-protocol IHeaderValue
  String
  (write-header! [this k io]
    (let [resp (:service/response io)]
      (.addHeader ^HttpServletResponse resp k this)))
  Long
  (write-header! [this k io]
    (let [resp (:service/response io)]
      (.addIntHeader ^HttpServletResponse resp k this)))
  java.util.Collection
  (write-header! [this k io]
    (let [resp (:service/response io)]
      (doseq [v this]
        (write-header! v k io)))))

(defn write-headers!
  [headers io]
  (doseq [[k v] headers]
    (write-header! v k io)))

(defprotocol IResponseBody
  (write-body! [this io]))

(extend-protocol IResponseBody
  ;; To extend, primitive array must be first.
  ;; Also can't refer to primitives directly.
  ;; https://clojure.atlassian.net/browse/CLJ-1381
  (Class/forName "[B")
  (write-body! [this io]
    (-> ^HttpServletResponse (:service/response io)
        (.getOutputStream)
        (.write ^bytes this)))
  String
  (write-body! [this io]
    (-> ^HttpServletResponse (:service/response io)
        (.getOutputStream)
        (.print this))))

(defprotocol IResponse
  (write-response! [this io]))

(extend-protocol IResponse
  java.util.Map
  (write-response! [this io]
    (let [^HttpServletResponse resp (:service/response io)]
      (if-let [status (:response/status this)]
        (.setStatus resp status))
      (if-let [headers (:response/headers this)]
        (write-headers! headers io))
      (if-let [body (:response/body this)]
        (write-body! body io)))))

;; Servlet

(defn context
  [servlet ^HttpServletRequest request response]
  {:service/servlet      servlet
   :service/request      request
   :service/response     response
   :request/method       (.getMethod request)
   :request/path         (.getRequestURI request)
   :request/query-string (.getQueryString request)})

(defn service-fn
  [handler]
  (fn [servlet request response]
    (let [ctx (context servlet request response)]
      (-> (handler ctx)
          (write-response! ctx)))))

(defn servlet*
  [service-fn]
  (proxy [HttpServlet] []
    (service [request response]
      (service-fn this request response))))

(defn servlet
  [handler]
  (servlet* (service-fn handler)))

;; Middleware

(defn map
  [f]
  (fn [handler]
    (fn [input]
      (handler (f input)))))

(defn terminate
  [pred f]
  (fn [handler]
    (fn [input]
      (if (pred input)
        (f input)
        (handler input)))))

;; Handler

(def chain-xf
  (clojure.core/map #(% identity)))

(defn chain
  [& xfs]
  (->> (into '() chain-xf xfs)
       (apply comp)))
