(ns dev.onionpancakes.serval.tests.jetty.test-jetty2
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty2 :as j]
            [clojure.test :refer [is deftest use-fixtures]])
  (:import [java.net.http HttpClient HttpRequest HttpResponse
            HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.net URI]
           [org.eclipse.jetty.server Server]))

;; Http client

(def ^HttpClient client
  (.build (HttpClient/newBuilder)))

(defn to-request
  [uri]
  (-> (HttpRequest/newBuilder)
      (.uri (URI. uri))
      (.method "GET" (HttpRequest$BodyPublishers/noBody))
      (.build)))

(defn ^HttpResponse send
  [req]
  (.send client (to-request req) (HttpResponse$BodyHandlers/ofString)))

;; Server

(defmacro with-server
  "Creates a server with config running in scope of expression."
  [config & body]
  `(let [server# (j/server ~config)]
     (.start server#)
     ~@body
     (.stop server#)))

;; Tests

(deftest test-minimal
  (with-server {:connectors [{:port 42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (str (.version resp)) "HTTP_1_1"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "foo")))))

(deftest test-http1
  (with-server {:connectors [{:protocol :http
                              :port     42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (str (.version resp)) "HTTP_1_1"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "foo")))))

(deftest test-http2c
  (with-server {:connectors [{:protocol :http2c
                              :port     42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (str (.version resp)) "HTTP_2"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "foo")))))

(deftest test-multiple-connectors
  (with-server {:connectors [{:protocol :http
                              :port     42000}
                             {:protocol :http2c
                              :port     42001}]
                :handler    #(case (int (:server-port (:serval.service/request %)))
                               42000 {:serval.response/body "foo"}
                               42001 {:serval.response/body "bar"})}
    (let [resp (send "http://localhost:42000")]
      (is (= (str (.version resp)) "HTTP_1_1"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "foo")))
    (let [resp (send "http://localhost:42001")]
      (is (= (str (.version resp)) "HTTP_2"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "bar")))))
