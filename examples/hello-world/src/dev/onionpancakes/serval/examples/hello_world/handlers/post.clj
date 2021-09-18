(ns dev.onionpancakes.serval.examples.hello-world.handlers.post
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jsonista :as srv.json]))

(defn post-response-handler
  [ctx]
  (let [req-body  (:serval.request/body ctx)
        resp-body (srv.json/json-body {:message "Json received!"
                                       :value   req-body})]
    {:serval.response/status       200
     :serval.response/content-type "application/json"
     :serval.response/body         resp-body}))

(defn post-error-handler
  [ctx]
  (let [error-msg (-> (:serval.jsonista/error ctx)
                      (:exception)
                      (.getMessage))
        resp-body (srv.json/json-body {:message "Bad Json."
                                       :error   error-msg})]
    {:serval.response/status       400
     :serval.response/content-type "application/json"
     :serval.response/body         resp-body}))

(def post-stack
  (comp (srv/map srv.json/read-json)
        (srv/terminate :serval.jsonista/error post-error-handler)
        (srv/map post-response-handler)))

(def post-handler
  (srv/handler post-stack))
