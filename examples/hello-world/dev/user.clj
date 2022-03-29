(ns user
  (:require [dev.onionpancakes.serval.examples.hello-world.server :as server]
            [dev.onionpancakes.serval.examples.hello-world.handlers :as handlers]))

(defn start []
  (server/start-dev))

(defn stop []
  (server/stop))
