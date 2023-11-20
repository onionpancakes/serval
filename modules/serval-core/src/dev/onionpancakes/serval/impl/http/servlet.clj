(ns dev.onionpancakes.serval.impl.http.servlet
  (:require [dev.onionpancakes.serval.impl.http.servlet-request
             :as impl.http.request]
            [dev.onionpancakes.serval.response.http
             :as response.http])
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
  [handler & args]
  (fn [servlet request response]
    (let [ctx (context servlet request response)
          ret (apply handler ctx args)]
      (response.http/set-response response ret))))

(defn servlet
  ^GenericServlet
  [handler & args]
  (-> (proxy [GenericServlet] [])
      (init-proxy {"service" (apply service-fn handler args)})))
