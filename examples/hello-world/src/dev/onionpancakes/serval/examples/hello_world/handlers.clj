(ns dev.onionpancakes.serval.examples.hello-world.handlers
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [dev.onionpancakes.serval.reitit :as srv.reitit]))

;; Basic example handlers

(defn hello-world-handler
  [ctx]
  {:serval.response/status 200
   :serval.response/body   "Hello World!"})

(defn json-handler
  [ctx]
  {:serval.response/status       200
   :serval.response/content-type "application/json"
   :serval.response/body         (-> {:message "Hello World!"}
                                     (srv.json/json-body))})

;; Example POST handler

(defn echo-json-response-handler
  [ctx]
  {:serval.response/status       200
   :serval.response/content-type "application/json"
   :serval.response/body         (-> {:message "Json received!"
                                      :value   (:serval.jsonista/value ctx)}
                                     (srv.json/json-body))})

(defn echo-json-error-handler
  [ctx]
  {:serval.response/status       400
   :serval.response/content-type "application/json"
   :serval.response/body         (-> {:message "Bad Json."
                                      :error   (-> (:serval.jsonista/error ctx)
                                                   (:exception)
                                                   (.getMessage))}
                                     (srv.json/json-body))})

(def echo-json-handler
  (srv/handler (comp (srv/map srv.json/read-json)
                     (srv/terminate :serval.jsonista/error echo-json-error-handler)
                     (srv/map echo-json-response-handler))))

;; Example route handler

(def routes
  [["/"     {:GET {:handler hello-world-handler}}]
   ["/json" {:GET {:handler json-handler}}]
   ["/echo" {:POST {:handler echo-json-handler}}]])

(def router
  (srv.reitit/router routes))

(defn not-found-handler
  [ctx]
  {:serval.response/status 404
   :serval.response/body   "Not found."})

(defn route-handler
  [ctx]
  (srv.reitit/route ctx router {:default not-found-handler}))
