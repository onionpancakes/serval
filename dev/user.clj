(ns user
  (:require [dev.onionpancakes.serval.core :as c]
            [dev.onionpancakes.serval.jetty :as j]
            [dev.onionpancakes.serval.reitit :as r]
            [reitit.core :as rt]
            [clojure.pprint :refer [pprint]])
  (:import [org.eclipse.jetty.server Server]))

(set! *warn-on-reflection* true)

(defn error
  [ctx]
  {:serval.response/status 400
   :serval.response/body   "Error!"})

(defn handle
  [ctx]
  (pprint ctx)
  (println (get-in ctx [:serval.service/request :headers "User-Agent"]))

  {:serval.response/status       200
   :serval.response/headers      {"Foo"          "Bar"
                                  "Test"         1
                                  "Test2"        [1 2 3]
                                  "Content-Type" "text/plain; charset=utf-8"}
   :serval.response/body         "Hello World! やばい"})

(def router
  (rt/router [["/"      {:GET {:key     :foo
                               :handler handle}}]
              ["/post" {:POST {:handler (fn [ctx]
                                          (println)
                                          (println :POST)
                                          (pprint ctx)
                                          ctx)}}]
              ["/error" {:GET {:handler error}}]
              ["/error/" {:GET {:handler error}}]]))

(def handler
  (r/route-handler router))

(def servlet
  (c/servlet #'handler))

(def config
  {:connectors [{:protocol :http
                 :port     3000}
                {:protocol :http2c
                 :port     3001}]
   :servlet    servlet})

(defonce ^Server server
  (j/server {}))

(defn restart []
  (doto server
    (.stop)
    (j/configure-server! config)
    (.start)))
