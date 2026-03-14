(ns user
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.servlet :as srv.servlet]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.serval.jetty-test :as srv.jetty-test]
            [dev.onionpancakes.serval.chassis :as srv.html]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [dev.onionpancakes.serval.transit :as srv.transit]
            [dev.onionpancakes.serval.examples.todo.server
             :as examples.todo.server]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]))

(defn my-handler
  [ctx]
  (doto ctx
    (srv/set-http :status 200)
    (srv/write-body "foobar" "lol")))

(defn my-redirect-handler
  [ctx]
  )

(defn my-throw-handler
  [ctx]
  (throw (ex-info "foooo" {})))

(defn my-error-handler
  [ctx]
  (srv/write-body ctx "Foobar"))

(defn my-filter
  [ctx]
  (srv/do-filter ctx)
  (srv/write-body ctx "after")
  (srv/send-error ctx 400))

(def routes
  [["" #'my-handler]
   ["/post" :POST #'my-handler]
   ["/redirect" #'my-redirect-handler]
   ["/filtered" #'my-filter #'my-handler]
   ["/throw" #'my-throw-handler]
   ["/error" #'my-error-handler]])

(def errors
  {400                        "/error"
   clojure.lang.ExceptionInfo "/error"})

(def app
  {:routes      routes
   :error-pages errors})

(def config
  {:connectors [{:protocol :http :port 3000}]
   :handler    app})

(defonce server
  (doto (srv.jetty/server)
    (srv.jetty/configure-server config)))

(defn configure-server
  ([]
   (configure-server config))
  ([config]
   (srv.jetty/configure-server server config)))

(defn start
  []
  (srv.jetty/start server))

(defn stop
  []
  (srv.jetty/stop server))

(defn restart
  ([]
   (srv.jetty/restart server))
  ([config]
   (srv.jetty/restart server config)))
