(ns dev.onionpancakes.serval.examples.hello-world.server
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.serval.reitit :as srv.reitit]
            [dev.onionpancakes.serval.examples.hello-world.handlers.hello-world :as handlers.hw]
            [dev.onionpancakes.serval.examples.hello-world.handlers.json :as handlers.js]
            [dev.onionpancakes.serval.examples.hello-world.handlers.post :as handlers.post]
            [dev.onionpancakes.serval.examples.hello-world.handlers.not-found :as handlers.nf]))

(def routes
  [["/" {:GET {:handler handlers.hw/hello-handler}}]
   ["/json" {:GET {:handler handlers.js/json-handler}}]
   ["/post" {:POST {:handler handlers.post/post-handler}}]])

(def router
  (srv.reitit/router routes))

(defn handler
  [ctx]
  (srv.reitit/route ctx router {:default handlers.nf/not-found-handler}))

(def servlet
  (srv/http-servlet handler))

;; Server

(def config
  {:connectors [{:protocol :http
                 :port     3000}]
   :servlets   [["/*" servlet]]})

(defn config-dev []
  (->> {:servlets [["/*" (srv/http-servlet #'handler)]]}
       (merge config)))

(defonce server
  (srv.jetty/server config))

(defn start []
  (doto server
    (.stop)
    (.start)))

(defn start-dev []
  (doto server
    (.stop)
    (srv.jetty/configure-server! (config-dev))
    (.start)))

(defn -main []
  (start))
