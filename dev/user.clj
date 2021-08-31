(ns user
  (:require [dev.onionpancakes.serval.core :as c]
            [dev.onionpancakes.serval.io.body :as io.body]
            [dev.onionpancakes.serval.io.http :as io.http]
            [dev.onionpancakes.serval.handler.http :as http]
            [dev.onionpancakes.serval.jetty :as j]
            [dev.onionpancakes.serval.reitit :as r]
            [dev.onionpancakes.serval.jsonista :as js]
            [reitit.core :as rt]
            [jsonista.core :as json]
            [clojure.pprint :refer [pprint]])
  (:import [org.eclipse.jetty.server Server]
           [java.util.concurrent CompletableFuture]))

(set! *warn-on-reflection* true)

(defn error
  [ctx]
  {:serval.response/status 400
   :serval.response/body   "Error!"})

(defn handle
  [ctx]
  (pprint ctx)
  (println (get-in ctx [:serval.service/request :headers "user-agent"]))
  (println (vec (seq (get-in ctx [:serval.service/request :header-names]))))
  (println (get-in ctx [:serval.service/request :method]))
  
  #_(println (get-in ctx [:serval.service/request :headers "User-Agent"]))
  #_(println (. (get ctx :serval.service/request) (getHeaders "User-Agent")))

  {:serval.response/status       200
   :serval.response/headers      {"Foo"          ["Bar"]
                                  "Test"         [1]
                                  "Test2"        [1 2 3]
                                  "Content-Type" ["text/plain"]}
   :serval.response/body         "Hello World! やばい foo bar baz"})

(defn handle-json
  [ctx]
  (http/response ctx 200 (js/json {:foo "bar"}) "application/json"))

(def post-xf
  (comp #_(c/map #(doto % (println)))
        (c/map js/read-json {:object-mapper json/keyword-keys-object-mapper})
        (c/terminate :serval.jsonista/error http/response 400 "Bad Json!" "text/plain" "utf-8")
        (c/map http/response 200 "Hi Post!!!" "text/plain" "utf-8")))

(def router
  (rt/router [["/"        {:GET {:key     :foo
                                 :handler handle}}]
              ["/foo"     {:GET {:handler handle-json}}]
              ["/foo/bar" {:GET {:handler handle}}]
              ["/foo/baz" {:GET {:handler handle}}]
              ["/post"    {:POST {:handler (c/handler post-xf)}}]
              ["/error"   {:GET {:handler error}}]
              ["/error/"  {:GET {:handler error}}]]))

(defn not-found
  [ctx]
  (http/response ctx 404 "Not Found lol?" "text/plain" "utf-8"))

(def handler-xf
  (comp (c/map r/match-by-path router)
        (c/map r/handle-match-by-method {:default not-found})))

(def handler
  (c/handler handler-xf))

(def http-servlet
  (c/http-servlet #'handler))

;;

(defn async-handler
  [ctx]
  (let [body (io.body/async-body "Async string lol やばい")
        resp {:serval.response/body               body
              :serval.response/content-type       "text/plain"
              :serval.response/character-encoding "utf-8"}]
    (-> (CompletableFuture/completedStage resp)
        (.thenApply (reify java.util.function.Function
                      (apply [_ input]
                        #_(throw (ex-info "foobar" {}))
                        input))))))

(def http-servlet2
  (c/http-servlet #'async-handler))

;;

(def config
  {:connectors  [{:protocol :http
                  :port     3000}
                 {:protocol :http2c
                  :port     3001}]
   :servlet     http-servlet2
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
