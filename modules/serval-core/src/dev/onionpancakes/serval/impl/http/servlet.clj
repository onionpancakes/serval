(ns dev.onionpancakes.serval.impl.http.servlet
  (:require [dev.onionpancakes.serval.impl.http.servlet-request
             :as impl.http.request]
            [dev.onionpancakes.serval.io.http :as io.http])
  (:import [java.util.function BiConsumer]
           [jakarta.servlet GenericServlet]
           [jakarta.servlet.http
            HttpServletRequest
            HttpServletResponse]))

(defn context
  [servlet request response]
  {:serval.service/servlet  servlet
   :serval.service/request  (impl.http.request/servlet-request-proxy request)
   :serval.service/response response})

(defn http-servlet
  ^GenericServlet
  [handler]
  (proxy [GenericServlet] []
    (service [^HttpServletRequest request ^HttpServletResponse response]
      (let [ctx (context this request response)
            ret (handler ctx)
            atx (cond
                  (.isAsyncStarted request)     (.getAsyncContext request)
                  (io.http/async-response? ret) (.startAsync request))]
        (.. (io.http/service-response ret this request response)
            (whenComplete (reify BiConsumer
                            (accept [_ _ throwable]
                              (if throwable
                                (->> (.getMessage ^Throwable throwable)
                                     (.sendError response 500)))
                              (if atx
                                (.complete atx))))))))))
