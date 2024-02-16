(ns dev.onionpancakes.serval.impl.servlet
  (:require [dev.onionpancakes.serval.core]
            [dev.onionpancakes.serval.impl.servlet-request :as impl.request]))

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

(defn servlet
  [service-fn]
  (ServalServlet. nil service-fn nil))
