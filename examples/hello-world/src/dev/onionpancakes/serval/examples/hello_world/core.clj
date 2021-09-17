(ns dev.onionpancakes.serval.examples.hello-world.core
  (:require [dev.onionpancakes.serval.core :as sc]
            [dev.onionpancakes.serval.jetty :as sj]))

(defn qhandler
  [ctx]
  {:serval.response/status 200
   :serval.response/body   "Hello World!"})

(def servlet
  (sc/http-servlet handler))

(defonce server
  (sj/server {:connectors [{:protocol :http
                            :port     3000}]
              :servlets   [["/*" servlet]]}))

(defn start []
  (doto server
    (.stop)
    (.start)))

(defn start-dev []
  (let [servlet-dev (sc/http-servlet #'handler)]
    (doto server
      (.stop)
      (sj/configure-server! {:servlets [["/*" servlet-dev]]})
      (.start))))
