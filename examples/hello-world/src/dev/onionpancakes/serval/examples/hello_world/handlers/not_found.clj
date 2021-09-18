(ns dev.onionpancakes.serval.examples.hello-world.handlers.not-found)

(defn not-found-handler
  [ctx]
  {:serval.response/status 404
   :serval.response/body   "Not found."})
