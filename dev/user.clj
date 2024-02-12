(ns user
  (:require [dev.onionpancakes.serval.core :as srv
             :refer [response]]
            [dev.onionpancakes.serval.servlet :as srv.servlet]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.serval.jetty.test :as srv.jetty.test]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [dev.onionpancakes.serval.transit :as srv.transit]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]))

(defn my-handler
  [ctx]
  (merge ctx {:serval.response/status             200
              :serval.response/headers            {}
              :serval.response/body               "Hello world!"
              :serval.response/content-type       "text/plain"
              :serval.response/character-encoding "utf-8"}))

(defn my-redirect-handler
  [ctx]
  (-> (srv/headers ctx {"foo" :bar})
      (srv/send-redirect "/")
      #_(srv/send-error 405 "FOOBAR")))

(defn my-throw-handler
  [ctx]
  (throw (ex-info "foooo" {})))

(defn my-error-handler
  [ctx]
  (response ctx 400 "It's foobar."))

(defn my-filter
  [ctx]
  (doto ctx
    (srv/set-response-kv! :serval.response/body "foo")
    (srv/do-filter-chain!)
    (srv/set-response-kv! :serval.response/body "bar")))

(def app
  {:routes      [["" #'my-handler]
                 ["/post" #{:POST} #'my-handler]
                 ["/redirect" #'my-redirect-handler]
                 ["/filtered" #'my-filter #'my-handler]
                 ["/throw" #'my-throw-handler]
                 ["/error" #'my-error-handler]]
   :error-pages {400                        "/error"
                 clojure.lang.ExceptionInfo "/error"}})

(def server-config
  {:connectors [{:protocol :http :port 3000}]
   :handler    app})

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
   (srv.jetty/configure-server server config)))

(defn restart
  ([]
   (srv.jetty/restart server))
  ([config]
   (srv.jetty/restart server config)))
