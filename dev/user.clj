(ns user
  (:require [dev.onionpancakes.serval.core :as c]
            [dev.onionpancakes.serval.jetty :as j])
  (:import [org.eclipse.jetty.server Server]))

(set! *warn-on-reflection* true)

(defn error
  [ctx]
  {:response/status 400
   :response/body   "Error!"})

(defn handle
  [ctx]
  {:response/status 200
   :response/body   "Hello World!"})

(def xf
  (comp (c/map #(assoc % :error (= (:request/path %) "/error")))
        (c/terminate :error error)))

(def handler
  (xf handle))

(defonce servlet
  (c/servlet #'handler))

(defonce ^Server server
  (j/server {:port     3000
             :servlets [["/" servlet]]}))

(defn restart []
  (.stop server)
  (.start server))
