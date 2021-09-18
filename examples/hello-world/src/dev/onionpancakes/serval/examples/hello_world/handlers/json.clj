(ns dev.onionpancakes.serval.examples.hello-world.handlers.json
  (:require [dev.onionpancakes.serval.jsonista :as srv.json]))

(defn json-handler
  [ctx]
  {:serval.response/status       200
   :serval.response/content-type "application/json"
   :serval.response/body         (srv.json/json-body
                                  {:message "Hello World!"})})
