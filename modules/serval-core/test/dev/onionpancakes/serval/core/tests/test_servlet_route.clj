(ns dev.onionpancakes.serval.core.tests.test-servlet-route
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.servlet.route :as srv.serlet.route]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is]]))

(defn example-filter-handler
  [ctx]
  (-> (srv/set-headers! ctx {"foo1" "bar1"})
      (srv/write-body! "pre-body:")
      (srv/do-filter-chain!)
      (srv/set-headers! {"foo2" "bar2"})
      (srv/write-body! ":post-body")))

(defn example-servlet-handler
  [ctx]
  (srv/response ctx 200 "main-body"))

(deftest test-routes
  (with-handler {:routes [["/filtered" example-filter-handler example-servlet-handler]
                          ["/servlet" example-servlet-handler]
                          ["/method" #{:GET} example-servlet-handler]]}
    (let [ret (send "http://localhost:42000/filtered")]
      (is (= (:status ret) 200))
      (is (= (get-in ret [:headers "foo1" 0]) "bar1"))
      (is (= (get-in ret [:headers "foo2" 0]) "bar2"))
      (is (= (:body ret) "pre-body:main-body:post-body")))
    (let [ret (send "http://localhost:42000/servlet")]
      (is (= (:status ret) 200))
      (is (= (:body ret) "main-body")))
    (let [ret (send "http://localhost:42000/method")]
      (is (= (:status ret) 200)))
    (let [ret (send {:method :POST
                     :uri    "http://localhost:42000/method"})]
      (is (= (:status ret) 405)))))

(deftest test-routes-with-url-filters
  (with-handler {:routes [["/filtered/*" example-filter-handler nil]
                          ["/filtered" example-servlet-handler]
                          ["/filtered/foo" example-servlet-handler]
                          ["/not-filtered" example-servlet-handler]]}
    (let [ret (send "http://localhost:42000/filtered")]
      (is (= (:status ret) 200))
      (is (= (get-in ret [:headers "foo1" 0]) "bar1"))
      (is (= (get-in ret [:headers "foo2" 0]) "bar2"))
      (is (= (:body ret) "pre-body:main-body:post-body")))
    (let [ret (send "http://localhost:42000/filtered/foo")]
      (is (= (:status ret) 200))
      (is (= (get-in ret [:headers "foo1" 0]) "bar1"))
      (is (= (get-in ret [:headers "foo2" 0]) "bar2"))
      (is (= (:body ret) "pre-body:main-body:post-body")))
    (let [ret (send "http://localhost:42000/not-filtered")]
      (is (= (:status ret) 200))
      (is (= (:body ret) "main-body")))))
