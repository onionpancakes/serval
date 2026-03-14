(ns dev.onionpancakes.serval.impl.servlet
  (:require [dev.onionpancakes.serval.impl.servlet-request :as impl.request]))

(defn context
  [servlet request response]
  {:serval.context/servlet  servlet
   :serval.context/request  (impl.request/servlet-request-proxy request)
   :serval.context/response response})

(deftype HandlerServlet [^:volatile-mutable config handler]
  jakarta.servlet.Servlet
  (init [_ conf]
    (set! config conf))
  (getServletConfig [_] config)
  (getServletInfo [_] "HandlerServlet")
  (service [this request response]
    (-> (context this request response)
        (handler)))
  (destroy [_]))

(defn handler-servlet
  [handler]
  (->HandlerServlet nil handler))

;;

(deftype ServalServlet [^:volatile-mutable servlet-config
                        service-fn
                        destroy-fn]
  jakarta.servlet.Servlet
  (init [_ config]
    (set! servlet-config config))
  (getServletConfig [_] servlet-config)
  (getServletInfo [_] "Serval Servlet")
  (service [this request response]
    (service-fn this request response))
  (destroy [this]
    (if (some? destroy-fn)
      (destroy-fn this))))

(defn wrapping-service-fn
  [service-fn]
  (fn [this request response]
    (service-fn this
                (impl.request/servlet-request-proxy request)
                response)))

(defn servlet*
  [service-fn]
  (ServalServlet. nil service-fn nil))

(defn servlet
  [service-fn]
  (ServalServlet. nil (wrapping-service-fn service-fn) nil))
