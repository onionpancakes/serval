(ns user
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.servlet :as srv.servlet]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.serval.jetty.test :as srv.jetty.test]
            [dev.onionpancakes.serval.chassis :as srv.html]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [dev.onionpancakes.serval.transit :as srv.transit]
            [dev.onionpancakes.serval.examples.todo.server
             :as examples.todo.server]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]))

(defn my-handler [_ req resp]
  (doto resp
    (srv/set-http :headers {:FOO "bar"}
                  :content-type "text/html")
    (srv/write-body "foobar" (:path-info req))))

(defn my-redirect-handler [_ _ resp]
  )

(defn my-throw-handler [_ _ _]
  (throw (ex-info "foooo" {})))

(defn my-error-handler [_ _ _]
  (srv/write-body "Foobar"))

(defn my-filter [_ _ _ _]
  )

(def app
  {:routes      [["" #'my-handler]
                 ["/post" #_#{:POST} #'my-handler]
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
