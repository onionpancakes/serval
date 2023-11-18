(ns dev.onionpancakes.serval.servlet
  (:refer-clojure :exclude [filter])
  (:require [dev.onionpancakes.serval.impl.http.servlet :as impl.http.servlet]
            [dev.onionpancakes.serval.impl.http.filter :as impl.http.filter])
  (:import [jakarta.servlet DispatcherType]
           [java.util EnumSet]))

(defn servlet
  "Creates a Servlet which services a http response from the handler function."
  ^jakarta.servlet.GenericServlet
  [handler]
  (impl.http.servlet/servlet handler))

(defn filter
  ^jakarta.servlet.GenericFilter
  [handler]
  (impl.http.filter/filter handler))
