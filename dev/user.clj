(ns user
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.handler.http
             :refer [response]]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.serval.reitit :as srv.reitit]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [promesa.core :as p]
            [clojure.pprint :refer [pprint]]))

(set! *warn-on-reflection* true)

(defn my-handler
  [ctx]
  ;; Assoc the response into ctx map.
  (into ctx {:serval.response/status       200
             :serval.response/body         "Hello world!"
             :serval.response/context-type "text/plain"}))

(defonce server
  (srv.jetty/server {:connectors [{:protocol :http
                                   :port     3000}]
                     :handler    #'my-handler}))

(defn start
  []
  (srv.jetty/start server))

(defn stop
  []
  (srv.jetty/stop server))

(defn configure
  [config]
  (srv.jetty/configure-server! server config))
