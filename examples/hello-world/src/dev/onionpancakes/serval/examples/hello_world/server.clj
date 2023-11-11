(ns dev.onionpancakes.serval.examples.hello-world.server
  (:require [dev.onionpancakes.serval.examples.hello-world.app :as app]
            [dev.onionpancakes.serval.jetty :as srv.jetty]))

;; Server

(defonce server
  (srv.jetty/server {:connectors [{:protocol :http :port 3000}]
                     :handler    app/route}))

(defn configure
  [{:keys [dev? port] :or {port 3000}}]
  (let [server-conf {:connectors [{:protocol :http :port port}]
                     :handler    (if dev? #'app/route app/route)}]
    (srv.jetty/configure-server server server-conf)))

(defn start []
  (srv.jetty/start server))

(defn stop []
  (srv.jetty/stop server))

(defn restart []
  (srv.jetty/restart server))
