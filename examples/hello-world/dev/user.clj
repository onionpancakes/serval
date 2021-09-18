(ns user
  (:require [dev.onionpancakes.serval.examples.hello-world.server :as server]))

(defn start []
  (server/start-dev))
