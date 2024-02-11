(ns user
  (:require [dev.onionpancakes.serval.examples.todo.app :as app]
            [dev.onionpancakes.serval.examples.todo.main :as main]
            [dev.onionpancakes.serval.examples.todo.server :as server
             :refer [start stop restart]]))

(defn configure-dev []
  (server/restart {:dev? true}))
