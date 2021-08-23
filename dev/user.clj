(ns user
  (:require [dev.onionpancakes.serval.core :as c]
            [dev.onionpancakes.serval.jetty :as j]
            [dev.onionpancakes.serval.reitit :as r]
            [reitit.core :as rt])
  (:import [org.eclipse.jetty.server Server]))

(set! *warn-on-reflection* true)

(defn error
  [ctx]
  {:serval.response/status 400
   :serval.response/body   "Error!"})

(defn handle
  [ctx]
  (pprint ctx)
  #_(println (get-in ctx [:serval.request/headers "User-Agent" 0] :foo))
  #_(println :body (slurp (:serval.request/body ctx)))
  #_(println (seq (:serval.request/headers ctx)))
  (println (:serval.request/header-names ctx))
  (println (-> (:serval.request/headers ctx)
               (get "Accept")))
  (println (seq (:serval.request/attribute-names ctx)))
  {:serval.response/status  200
   :serval.response/headers {"Foo"   "Bar"
                             "Test"  1
                             "Test2" [1 2 3]
                             "Content-Type" "text/plain; charset=UTF-8"}
   :serval.response/body    "Hello World! やばい"})

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

(def xf
  (comp (c/map #(assoc % :error (= (:serval.request/path %) "/error")))
        (c/terminate :error error)))

(def handler
  (r/route-handler router)
  #_(xf handle))

(defonce servlet
  (c/servlet #'handler))

(defonce ^Server server
  (j/server {:connectors [{:protocol :http
                           :port     3000}
                          {:protocol :http2c
                           :port     3001}]
             :handler    #'handler}))

(defn restart []
  (.stop server)
  (.start server))
