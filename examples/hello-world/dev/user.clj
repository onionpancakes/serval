(ns user
  (:require [dev.onionpancakes.serval.examples.hello-world.app :as app]
            [dev.onionpancakes.serval.examples.hello-world.server :as server
             :refer [start stop restart]]))

(defn init-dev []
  (server/configure {:dev? true}))
