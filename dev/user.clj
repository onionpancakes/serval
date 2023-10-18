(ns user
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.handler.http
             :refer [response]]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.serval.reitit :as srv.reitit]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [dev.onionpancakes.serval.transit :as srv.transit]
            [promesa.core :as p]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(defn my-handler
  [ctx]
  (into ctx {:serval.response/status             200
             :serval.response/headers            {}
             :serval.response/body               "Hello world!"
             :serval.response/content-type       "text/plain"
             :serval.response/character-encoding "utf-8"}))

(def server-config
  {:connectors [{:protocol :http :port 3000}]
   :handler    #'my-handler})

(defonce server
  (srv.jetty/server server-config))

(defn start
  []
  (srv.jetty/start server))

(defn stop
  []
  (srv.jetty/stop server))

(defn configure
  ([]
   (configure server-config))
  ([config]
   (srv.jetty/configure-server! server config)))
