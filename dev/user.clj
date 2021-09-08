(ns user
  (:require [dev.onionpancakes.serval.core :as c]
            [dev.onionpancakes.serval.io.body :as io.body]
            [dev.onionpancakes.serval.io.http :as io.http]
            [dev.onionpancakes.serval.handler.http :as http]
            [dev.onionpancakes.serval.jetty :as j]
            [dev.onionpancakes.serval.reitit :as r]
            [dev.onionpancakes.serval.jsonista :as js]
            [dev.onionpancakes.serval.mock :as mock]
            [dev.onionpancakes.serval.tests.core.test-core-handler :as tc]
            [dev.onionpancakes.serval.tests.core.test-http-handler :as th]
            [dev.onionpancakes.serval.tests.core.test-body-io :as tbio]
            [dev.onionpancakes.serval.tests.core.test-http-io :as thttp]
            [dev.onionpancakes.serval.tests.mock.test-http :as mhttp]
            [reitit.core :as rt]
            [jsonista.core :as json]
            [promesa.core :as p]
            [clojure.pprint :refer [pprint]])
  (:import [dev.onionpancakes.serval.io.body BytesReadChunk]
           [org.eclipse.jetty.server Server]
           [java.util.concurrent CompletableFuture]))

(set! *warn-on-reflection* true)

(defn error
  [ctx]
  {:serval.response/status 400
   :serval.response/body   "Error!"})

(defn handle
  [ctx]
  #_(pprint ctx)
  #_(println (get-in ctx [:serval.service/request :headers "Cookie"]))
  #_(println (get-in ctx [:serval.service/request :protocol]))
  #_(println (get-in ctx [:serval.service/request :locales]))
  #_(println (get-in ctx [:serval.service/request :cookies]))
  #_(doseq [c (get-in ctx [:serval.service/request :cookies])]
    (println :cookie ))

  #_(println (.getCookies (get ctx :serval.service/request)))

  {:serval.response/status  200
   #_#_:serval.response/cookies (get-in ctx [:serval.service/request :cookies])
   :serval.response/headers {"Foo"          ["Bar"]
                             "Test"         [1]
                             "Test2"        [1 2 3]
                             "Content-Type" ["text/plain"]
                             #_#_"Set-Cookie"  ["Foobar=baz; SameSite=None; Secure"]}
   :serval.response/body    "Hello World! やばい foo bar baz"})

(defn handle-json
  [ctx]
  (http/response ctx 200 (js/json {:foo "bar"}) "application/json"))

(def post-xf
  (comp #_(c/map #(doto % (println)))
        (c/map js/read-json {:object-mapper json/keyword-keys-object-mapper})
        (c/terminate :serval.jsonista/error http/response 400 "Bad Json!" "text/plain" "utf-8")
        (c/map http/response 200 "Hi Post!!!" "text/plain" "utf-8")))

(def router
  (rt/router [["/"        {:GET  {:key     :foo
                                  :handler handle}
                           :POST {:handler handle}}]
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
  (let [body  (io.body/async-body "Async string lol やばい")
        body2 (io.body/async-body (js/json {:foo "bar"}))
        resp  {:serval.response/body               body2
               :serval.response/content-type       "application/json"
               :serval.response/character-encoding "utf-8"}]
    (-> (CompletableFuture/completedStage resp)
        (.thenApply (reify java.util.function.Function
                      (apply [_ input]
                        #_(throw (ex-info "foobar" {}))
                        input))))))

(defn async-handler-post
  [ctx]
  (let [req (:serval.service/request ctx)]
    (-> ^CompletableFuture (io.body/read-body-as-bytes-async! req)
        (.thenApply (reify java.util.function.Function
                      (apply [_ input]
                        (http/response ctx 200 (io.body/async-body input) "application/json" "utf-8")))))))

(defn async-handler-promise
  [ctx]
  (-> (:serval.service/request ctx)
      (io.body/read-body-as-bytes-async!)
      (p/then #(http/response ctx 200 (io.body/async-body %) "application/json"))))

(def http-servlet2
  (c/http-servlet #'async-handler-promise))

;;

(def config
  {:connectors [{:protocol :http
                 :port     3000}
                {:protocol :http2c
                 :port     3001}]
   :servlets   [["/*" http-servlet {:multipart {:location "/tmp"}}]]
   :gzip       {:included-methods    [:GET :POST]
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
