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

(defn service-fn
  [handler]
  (fn [servlet ^HttpServletRequest request ^HttpServletResponse response]
    (let [ctx (context servlet request response)
          ret (handler ctx)
          _   (if (io.http/async-response? ret)
                (.startAsync request))]
      (-> (io.http/service-response ret servlet request response)
          (io.http/complete-response servlet request response)))))

(defn http-servlet
  ^GenericServlet
  [handler]
  (-> (proxy [GenericServlet] [])
      (init-proxy {"service" (service-fn handler)})))
