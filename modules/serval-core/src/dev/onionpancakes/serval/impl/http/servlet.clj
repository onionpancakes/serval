(ns dev.onionpancakes.serval.impl.http.servlet
  (:require [dev.onionpancakes.serval.impl.http.servlet-request
             :as impl.http.request]
            [dev.onionpancakes.serval.io.http :as io.http])
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [java.util.function Function BiConsumer]
           [jakarta.servlet GenericServlet]
           [jakarta.servlet.http
            HttpServletRequest
            HttpServletResponse]))

(defn context
  [servlet request response]
  {:serval.service/servlet  servlet
   :serval.service/request  (impl.http.request/servlet-request-proxy request)
   :serval.service/response response})

(defn complete-async
  [^CompletionStage cstage ^HttpServletRequest request ^HttpServletResponse response]
  (when-some [atx (cond
                    (.isAsyncStarted request) (.getAsyncContext request)
                    (some? cstage)            (.startAsync request))]
    (if cstage
      (.whenComplete cstage (reify BiConsumer
                              (accept [_ _ throwable]
                                (if throwable
                                  (->> (.getMessage ^Throwable throwable)
                                       (.sendError response 500)))
                                (.complete atx))))
      (.complete atx))))

(defn http-service-fn
  [handler]
  (fn [servlet request response]
    (-> (context servlet request response)
        (handler)
        (io.http/service-response servlet request response)
        (complete-async request response))))

(defn http-servlet
  ^GenericServlet
  [handler]
  (let [service-fn (http-service-fn handler)]
    (proxy [GenericServlet] []
      (service [request response]
        (service-fn this request response)))))
