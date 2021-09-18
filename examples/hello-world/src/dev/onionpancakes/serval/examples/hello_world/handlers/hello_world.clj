(ns dev.onionpancakes.serval.examples.hello-world.handlers.hello-world)

(defn hello-handler
  [ctx]
  {:serval.response/status 200
   :serval.response/body   "Hello World!"})
