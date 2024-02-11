(ns dev.onionpancakes.serval.examples.todo.main
  (:gen-class)
  (:require [dev.onionpancakes.serval.examples.todo.server :as server]))

(defn read-env
  []
  (->> {:port (System/getenv "PORT")}
       (into {} (filter val))))

(defn -main
  []
  (server/configure (read-env))
  (server/start))
