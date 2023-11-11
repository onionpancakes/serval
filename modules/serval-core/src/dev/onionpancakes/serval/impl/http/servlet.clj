(ns dev.onionpancakes.serval.impl.http.servlet
  (:require [dev.onionpancakes.serval.impl.http.servlet-request
             :as impl.http.request]
            [dev.onionpancakes.serval.service.http
             :as service.http])
  (:import [java.util.function BiConsumer]
           [jakarta.servlet GenericServlet]
           [jakarta.servlet.http
            HttpServletRequest
            HttpServletResponse]))

(defn context
  [servlet request response]
  {:serval.context/servlet  servlet
   :serval.context/request  (impl.http.request/servlet-request-proxy request)
   :serval.context/response response})

(defn service-fn
  [handler]
  (fn [servlet ^HttpServletRequest request ^HttpServletResponse response]
    (-> (context servlet request response)
        (handler)
        (service.http/service-response servlet request response))))

(defn servlet
  ^GenericServlet
  [handler]
  (-> (proxy [GenericServlet] [])
      (init-proxy {"service" (service-fn handler)})))
