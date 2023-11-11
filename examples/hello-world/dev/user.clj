(ns user
  (:require [dev.onionpancakes.serval.examples.hello-world.server :as server]
            [dev.onionpancakes.serval.examples.hello-world.app :as app]))

(defn init-dev []
  (server/configure {:dev? true}))

(defn start []
  (server/start))

(defn stop []
  (server/stop))
