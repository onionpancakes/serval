(ns dev.onionpancakes.serval.tests.core.test-http-response
  (:require [dev.onionpancakes.serval.io.http :as http]
            [dev.onionpancakes.serval.io.body :as body]
            [dev.onionpancakes.serval.mock.http :as mock]
            [clojure.test :refer [deftest are is]])
  (:import [java.util.concurrent CompletionStage CompletableFuture]))

(def response
  {:serval.response/status  200
   :serval.response/headers {"Foo" [0 (int 1) "Bar"]}
   :serval.response/body    "Foobar"})

(deftest test-response
  (let [req  (mock/http-servlet-request {} "")
        resp (mock/http-servlet-response {} req)
        ctx  {:serval.service/request  req
              :serval.service/response resp}
        ret  (http/write-response response ctx)
        ;; Hmmm... Flushing should not be a detail of concern?
        _    (.flush (:writer @(:data resp)))]
    (is (= (:status @(:data resp) 200)))
    (is (= (:headers @(:data resp)) {"Foo" [0 1 "Bar"]}))
    ;; OutputStream here requires flush to see data.
    (is (= (slurp (.toByteArray (:output-stream resp))) "Foobar"))))

(def response-async-body
  {:serval.response/status 200
   :serval.response/body   (body/async-body "Foobar")})

(deftest test-response-async-body
  (let [req  (mock/http-servlet-request {} "")
        resp (mock/http-servlet-response {} req)
        ctx  {:serval.service/request  req
              :serval.service/response resp}
        _    (.startAsync req)
        ret  (http/write-response response-async-body ctx)]
    (is (http/async-response? response-async-body ctx))
    (is (instance? CompletionStage ret))
    (.get (.toCompletableFuture ret)) ; Wait till resolved
    (is (= (:status @(:data resp) 200)))
    (is (= (slurp (.toByteArray (:output-stream resp))) "Foobar"))))

(def response-cf
  (-> {:serval.response/status 200
       :serval.response/body   "Foobar"}
      (CompletableFuture/completedStage)))

(deftest test-response-cf
  (let [req  (mock/http-servlet-request {} "")
        resp (mock/http-servlet-response {} req)
        ctx  {:serval.service/request  req
              :serval.service/response resp}
        _    (.startAsync req)
        ret  (http/write-response response-cf ctx)
        ;; Flush
        _    (.flush (:writer @(:data resp)))]
    (is (http/async-response? response-async-body ctx))
    (is (instance? CompletionStage ret))
    (.get (.toCompletableFuture ret))
    (is (= (:status @(:data resp) 200)))
    (is (= (slurp (.toByteArray (:output-stream resp))) "Foobar"))))
