(ns dev.onionpancakes.serval.examples.hello-world.server
  (:require [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.serval.examples.hello-world.handlers :refer [route-handler]]))

;; Server

(defonce server
  (srv.jetty/server {:connectors [{:protocol :http
                                   :port     3000}]
                     :handler    route-handler}))

(defn start []
  (doto server
    (.stop)
    (.start)))

(defn start-dev []
  (doto server
    (.stop)
    (srv.jetty/configure-server! {:handler #'route-handler})
    (.start)))

(defn stop []
  (.stop server))

(defn -main []
  (start))
