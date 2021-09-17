(ns dev.onionpancakes.serval.examples.hello-world.core
  (:require [dev.onionpancakes.serval.core :as sc]
            [dev.onionpancakes.serval.jetty :as sj]))

(defn handler
  [ctx]
  {:serval.response/status 200
   :serval.response/body   "Hello World!"})

(def servlet
  (sc/http-servlet handler))

(def conf
  {:connectors [{:protocol :http
                 :port     3000}]
   :servlets   [["/*" servlet]]})

(defonce server
  (sj/server conf))

(defn start []
  (doto server
    (.stop)
    (.start)))

(defn start-dev []
  (let [servlet-dev (sc/http-servlet #'handler)
        conf-dev    (merge conf {:servlets [["/*" servlet-dev]]})]
    (doto server
      (.stop)
      (sj/configure-server! conf-dev)
      (.start))))
