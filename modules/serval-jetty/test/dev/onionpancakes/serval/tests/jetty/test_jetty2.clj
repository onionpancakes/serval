(ns dev.onionpancakes.serval.tests.jetty.test-jetty2
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty2 :as j]
            [clojure.test :refer [is deftest use-fixtures]])
  (:import [java.net.http HttpClient HttpRequest
            HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.net URI]
           [org.eclipse.jetty.server Server]))

(defn handler
  [ctx]
  {:serval.response/body "foo"})

(def ^Server server
  (j/server {:connectors [{:port 42000}]
             :handler    handler}))

(defn with-server-started-fixture
  [f]
  (.start server)
  (f)
  (.stop server))

(use-fixtures :once with-server-started-fixture)

;; Http client

(def client
  (.build (HttpClient/newBuilder)))

(def request
  (-> (HttpRequest/newBuilder)
      (.uri (URI. "http://localhost:42000"))
      (.method "GET" (HttpRequest$BodyPublishers/noBody))
      (.build)))

(defn send
  [^HttpClient client req]
  (let [resp (.send client req (HttpResponse$BodyHandlers/ofString))]
    {:status (.statusCode resp)
     :body   (.body resp)}))

;; Tests

(deftest test-server
  (let [resp (send client request)]
    (is (= {:status 200 :body "foo"} resp))))
