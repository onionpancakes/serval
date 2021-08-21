(ns user
  (:require [dev.onionpancakes.serval.core :as c]
            [dev.onionpancakes.serval.jetty :as j]
            [dev.onionpancakes.serval.reitit :as r]
            [reitit.core :as rt])
  (:import [org.eclipse.jetty.server Server]))

(set! *warn-on-reflection* true)

(defn error
  [ctx]
  (pprint ctx)
  {:serval.response/status 400
   :serval.response/body   "Error!"})

(defn handle
  [ctx]
  (pprint ctx)
  {:serval.response/status  200
   :serval.response/headers {"Foo"   "Bar"
                             "Test"  1
                             "Test2" [1 2 3]}
   :serval.response/body    "Hello World!"})

(def router
  (rt/router [["/"      {:GET {:key     :foo
                               :handler handle}}]
              ["/error" {:GET {:handler error}}]]))

(def xf
  (comp (c/map #(assoc % :error (= (:serval.request/path %) "/error")))
        (c/terminate :error error)))

(def handler
  (r/route-handler router)
  #_(xf handle))

(defonce servlet
  (c/servlet #'handler))

(defonce ^Server server
  (j/server {:connectors [{:type :http
                           :port 3000}]
             :context    [["/" servlet]]}))

(defn restart []
  (.stop server)
  (.start server))
