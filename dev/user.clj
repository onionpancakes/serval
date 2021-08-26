(ns user
  (:require [dev.onionpancakes.serval.core :as c]
            [dev.onionpancakes.serval.jetty :as j]
            [dev.onionpancakes.serval.reitit :as r]
            [dev.onionpancakes.serval.jsonista :as js]
            [reitit.core :as rt]
            [jsonista.core :as json]
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
  #_(println (get-in ctx [:serval.service/request :headers "User-Agent"]))
  
  #_(println (get-in ctx [:serval.service/request :headers "User-Agent"]))
  #_(println (. (get ctx :serval.service/request) (getHeaders "User-Agent")))

  {:serval.response/status       200
   :serval.response/headers      {"Foo"          "Bar"
                                  "Test"         1
                                  "Test2"        [1 2 3]
                                  "Content-Type" "text/plain; charset=utf-8"}
   :serval.response/body         "Hello World! やばい foo bar baz"})

(defn handle-post
  [ctx]
  (println ctx)
  (conj ctx {:serval.response/status 200
             :serval.response/body   "Hi post!"}))

(def post-xf
  (comp (c/map js/read-json {:object-mapper json/keyword-keys-object-mapper})
        (c/map handle-post)))

(def router
  (rt/router [["/"      {:GET {:key     :foo
                               :handler handle}}]
              ["/foo" {:GET {:handler handle}}]
              ["/foo/bar" {:GET {:handler handle}}]
              ["/foo/baz" {:GET {:handler handle}}]
              ["/post" {:POST {:handler (c/handler post-xf)}}]
              ["/error" {:GET {:handler error}}]
              ["/error/" {:GET {:handler error}}]]))

(def handler
  (r/route-handler router))

(def servlet
  (c/servlet #'handler))

(def config
  {:connectors  [{:protocol :http
                  :port     3000}
                 {:protocol :http2c
                  :port     3001}]
   :servlet     servlet
   :gzip        {:included-methods    [:GET :POST]
                 :included-mime-types ["text/plain"]
                 :included-paths      ["/*"]
                 :excluded-methods    []
                 :excluded-mime-types []
                 :excluded-paths      []}})

(defonce ^Server server
  (j/server {:thread-pool {:min-threads  1
                           :max-threads  8}}))

(defn restart []
  (doto server
    (.stop)
    (j/configure-server! config)
    (.start)))
