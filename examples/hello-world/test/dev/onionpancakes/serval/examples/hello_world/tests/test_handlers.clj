(ns dev.onionpancakes.serval.examples.hello-world.tests.test-handlers
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.examples.hello-world.app :as app]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is testing]]
            [jsonista.core :as json]))

(deftest test-hello-world
  (with-handler app/hello-world
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:body resp) "Hello World!")))))

(deftest test-hello-world-json
  (with-handler app/hello-world-json
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (-> (:body resp)
                 (json/read-value json/keyword-keys-object-mapper))
             {:message "Hello World!"})))))

(deftest test-echo-json
  (with-handler app/echo-json
    (testing "Echo json success"
      (let [value {:foo "bar"}
            resp  (send {:method :POST
                         :body   (json/write-value-as-bytes value)})]
        (is (= (:status resp) 200))
        (is (= (-> (:body resp)
                   (json/read-value json/keyword-keys-object-mapper)
                   (:value))
               value))))
    (testing "Echo json error"
      (let [value "{bad json value}"
            resp  (send {:method :POST
                         :body   value})]
        (is (= (:status resp) 400))
        (is (-> (:body resp)
                (json/read-value json/keyword-keys-object-mapper)
                (map?)))))))

(deftest test-not-found
  (with-handler app/not-found
    (let [resp (send)]
      (is (= (:status resp) 404)))))
