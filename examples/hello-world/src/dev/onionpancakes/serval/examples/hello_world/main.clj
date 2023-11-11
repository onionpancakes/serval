(ns dev.onionpancakes.serval.examples.hello-world.main
  (:require [dev.onionpancakes.serval.examples.hello-world.server
             :as server]))

(defn -main
  []
  (server/start))
