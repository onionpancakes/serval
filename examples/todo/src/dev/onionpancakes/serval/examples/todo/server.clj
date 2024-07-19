(ns dev.onionpancakes.serval.examples.todo.server
  (:require [dev.onionpancakes.serval.examples.todo.app :as app]
            [dev.onionpancakes.serval.jetty :as srv.jetty]))

(defonce server
  (srv.jetty/server))

(defn server-config
  [{:keys [dev? port] :or {port 3000}}]
  {:connectors [{:protocol :http2c :port port}]
   :handler    (if dev? app/app-dev app/app)})

(defn configure
  [conf]
  (->> (server-config conf)
       (srv.jetty/configure-server server)))

(defn start []
  (srv.jetty/start server))

(defn stop []
  (srv.jetty/stop server))

(defn restart
  ([]
   (srv.jetty/restart server))
  ([conf]
   (srv.jetty/restart server (server-config conf))))
