(ns dev.onionpancakes.serval.examples.todo.app
  (:require [dev.onionpancakes.serval.core :as srv]))

(defn hello-world
  [ctx]
  (srv/response ctx 200 "Hello world!!!"))

(def app
  {:routes [["" hello-world]]})

(def app-dev
  {:routes [["" #'hello-world]]})
