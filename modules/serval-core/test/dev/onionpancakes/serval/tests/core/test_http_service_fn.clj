(ns dev.onionpancakes.serval.tests.core.test-http-service-fn
  (:require [dev.onionpancakes.serval.io.http :as http]
            [dev.onionpancakes.serval.mock.http :as mock]
            [clojure.test :refer [deftest is]])
  (:import [java.util.concurrent CompletionStage CompletableFuture]
           [jakarta.servlet ServletRequest]))

(defn handler-sync
  [ctx]
  {:serval.response/status 200})

(def service-sync
  (http/service-fn handler-sync))

(deftest test-sync
  (let [req  (mock/http-servlet-request {} "")
        resp (mock/http-servlet-response {} req)
        _    (service-sync nil req resp)]
    (is (not (.isAsyncStarted req)))))

(defn handler-async
  [ctx]
  (.startAsync ^ServletRequest (:serval.service/request ctx))
  {:serval.response/status 200})

(def service-async
  (http/service-fn handler-async))

(deftest test-async
  (let [req  (mock/http-servlet-request {} "")
        resp (mock/http-servlet-response {} req)
        ret  (service-async nil req resp)
        _    (.get (.toCompletableFuture ^CompletionStage ret))]
    ;; Async context instance should exist and should be completed.
    (is (-> req :data deref :async-context :data deref :async-complete?))))

(defn handler-async-cf
  [ctx]
  (CompletableFuture/completedStage {:serval.response/status 200}))

(def service-async-cf
  (http/service-fn handler-async-cf))

(deftest test-async-cf
  (let [req  (mock/http-servlet-request {} "")
        resp (mock/http-servlet-response {} req)
        ret  (service-async-cf nil req resp)
        _    (.get (.toCompletableFuture ^CompletionStage ret))]
    ;; Async context instance should exist and should be completed.
    (is (-> req :data deref :async-context :data deref :async-complete?))))
