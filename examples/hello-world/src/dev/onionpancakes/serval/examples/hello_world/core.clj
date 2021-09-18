(ns dev.onionpancakes.serval.examples.hello-world.core
  (:require [dev.onionpancakes.serval.core :as sc]
            [dev.onionpancakes.serval.jetty :as sj]
            [dev.onionpancakes.serval.reitit :as sr]
            [dev.onionpancakes.serval.jsonista :as json]
            [reitit.core :as r]))

;; Handlers

(defn hello-handler
  [ctx]
  {:serval.response/status 200
   :serval.response/body   "Hello World!"})

(defn json-handler
  [ctx]
  {:serval.response/status       200
   :serval.response/content-type "application/json"
   :serval.response/body         (json/json-body {:message "Hello World!"})})

(defn post-handler
  [ctx]
  (let [req-body  (:serval.request/body ctx)
        resp-body (json/json-body {:message "Json received!"
                                   :value   req-body})]
    {:serval.response/status       200
     :serval.response/content-type "application/json"
     :serval.response/body         resp-body}))

(defn post-error-handler
  [ctx]
  (let [error-msg (-> (:serval.jsonista/error ctx)
                      (:exception)
                      (.getMessage))]
    {:serval.response/status       400
     :serval.response/content-type "application/json"
     :serval.response/body         (json/json-body {:message "Bad Json."
                                                    :error   error-msg})}))

(def post-stack
  (comp (sc/map json/read-json)
        (sc/terminate :serval.jsonista/error post-error-handler)
        (sc/map post-handler)))

(defn not-found-handler
  [ctx]
  {:serval.response/status 404
   :serval.response/body   "Not found."})

(def router
  (r/router [["/" {:GET {:handler hello-handler}}]
             ["/json" {:GET {:handler json-handler}}]
             ["/post" {:POST {:handler (sc/handler post-stack)}}]]))

(defn handler
  [ctx]
  (sr/route ctx router {:default not-found-handler}))

;; Server

(def servlet
  (sc/http-servlet handler))

(def conf
  {:connectors [{:protocol :http
                 :port     3000}]
   :servlets   [["/*" servlet]]})

(defonce server
  (sj/server conf))

(defn start []
  (doto server
    (.stop)
    (.start)))

(defn conf-dev []
  (let [servlet-dev (sc/http-servlet #'handler)]
    (merge conf {:servlets [["/*" servlet-dev]]})))

(defn start-dev []
  (doto server
    (.stop)
    (sj/configure-server! (conf-dev))
    (.start)))
