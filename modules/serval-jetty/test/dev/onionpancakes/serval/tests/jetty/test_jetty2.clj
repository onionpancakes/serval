(ns dev.onionpancakes.serval.tests.jetty.test-jetty2
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty2 :as j]
            [clojure.test :refer [is deftest use-fixtures]])
  (:import [java.net.http HttpClient HttpRequest HttpResponse
            HttpRequest$Builder
            HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.net URI]
           [java.io ByteArrayInputStream]
           [java.util.zip GZIPInputStream]
           [org.eclipse.jetty.server Server]
           [org.eclipse.jetty.server.handler.gzip GzipHandler]))

;; Http client

(def ^HttpClient client
  (.build (HttpClient/newBuilder)))

(defn ^HttpRequest$Builder set-headers
  [^HttpRequest$Builder builder headers]
  (doseq [[header-key values] headers
          :let                [header-name (name header-key)]
          value               values]
    (.setHeader builder header-name value))
  builder)

(defn to-request
  [req]
  (-> (HttpRequest/newBuilder)
      (.uri (URI. (:uri req)))
      (.method "GET" (HttpRequest$BodyPublishers/noBody))
      (set-headers (:headers req))
      (.build)))

(defn ^HttpResponse send
  ([req]
   (.send client (to-request req) (HttpResponse$BodyHandlers/ofString)))
  ([req bh]
   (.send client (to-request req) bh)))

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
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (str (.version resp)) "HTTP_1_1"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "foo")))))

(deftest test-http1
  (with-server {:connectors [{:protocol :http
                              :port     42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (str (.version resp)) "HTTP_1_1"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "foo")))))

(deftest test-http2c
  (with-server {:connectors [{:protocol :http2c
                              :port     42000}]
                :handler    (constantly {:serval.response/body "foo"})}
    (let [resp (send {:uri "http://localhost:42000"})]
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
    (let [resp (send {:uri "http://localhost:42000"})]
      (is (= (str (.version resp)) "HTTP_1_1"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "foo")))
    (let [resp (send {:uri "http://localhost:42001"})]
      (is (= (str (.version resp)) "HTTP_2"))
      (is (= (.statusCode resp) 200))
      (is (= (.body resp) "bar")))))

(defn decompress
  "Decompress bytes into a String."
  [^bytes b]
  (slurp (GZIPInputStream. (ByteArrayInputStream. b))))

(deftest test-gzip-handler
  (with-server {:connectors [{:protocol :http
                              :port     42000}]
                :handler    (-> (constantly {:serval.response/body "foo"})
                                (j/gzip-handler {:min-gzip-size 0}))}
    (let [resp (send {:uri     "http://localhost:42000"
                      :headers {:accept-encoding ["gzip"]}}
                     (HttpResponse$BodyHandlers/ofByteArray))]
      (is (= (.statusCode resp) 200))
      (is (= (decompress (.body resp)) "foo")))))
