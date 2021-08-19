(ns user
  (:require [dev.onionpancakes.serval.core :as c]
            [dev.onionpancakes.serval.jetty :as j])
  (:import [org.eclipse.jetty.server Server]))

(set! *warn-on-reflection* true)

(defn handler
  [ctx]
  {:response/status 200
   :response/body   "Hello World!"})

(def servlet
  (c/servlet #'handler))

(defonce ^Server server
  (j/server {:port     3000
             :servlets [["/" servlet]]}))

(defn restart []
  (.stop server)
  (.start server))
