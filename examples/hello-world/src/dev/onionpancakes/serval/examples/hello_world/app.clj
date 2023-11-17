(ns dev.onionpancakes.serval.examples.hello-world.app
  (:require [dev.onionpancakes.serval.core :as srv
             :refer [response]]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [dev.onionpancakes.serval.reitit :as srv.reitit]))

;; Basic example handlers

(defn hello-world
  [ctx]
  (response ctx 200 "Hello World!"))

(defn hello-world-json
  [ctx]
  (let [data {:message "Hello World!"}]
    (response ctx 200 (srv.json/json-value data) "application/json")))

;; Example POST handler

(defn echo-json-success
  [ctx]
  (let [value (:serval.jsonista/value ctx)
        data  {:message "Json received!"
               :value   value}]
    (response ctx 200 (srv.json/json-value data) "application/json")))

(defn echo-json-error
  [ctx]
  (let [error (.getMessage (:serval.jsonista/error ctx))
        data  {:message "Bad Json."
               :error   error}]
    (response ctx 400 (srv.json/json-value data) "application/json")))

(def echo-json
  (srv/handler (comp (srv/map srv.json/read-json)
                     (srv/terminate :serval.jsonista/error echo-json-error)
                     (srv/map echo-json-success))))

;; Example route handler

(def routes
  [["/"     {:GET {:handler hello-world}}]
   ["/json" {:GET {:handler hello-world-json}}]
   ["/echo" {:POST {:handler echo-json}}]])

(def router
  (srv.reitit/router routes))

(defn not-found
  [ctx]
  (response ctx 404 "Not found."))

(defn route
  [ctx]
  (srv.reitit/route ctx router {:default not-found}))
