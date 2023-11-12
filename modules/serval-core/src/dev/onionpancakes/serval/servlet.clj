(ns dev.onionpancakes.serval.servlet
  (:require [dev.onionpancakes.serval.impl.http.servlet :as impl.http.servlet]))

(defn servlet
  "Creates a Servlet which services a http response from the handler function."
  ^jakarta.servlet.GenericServlet
  [handler]
  (impl.http.servlet/servlet handler))
