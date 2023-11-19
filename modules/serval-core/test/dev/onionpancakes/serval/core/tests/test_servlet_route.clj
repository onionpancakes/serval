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
      (srv/set-body! "pre-body:")
      (srv/do-filter-chain!)
      (srv/set-headers! {"foo2" "bar2"})
      (srv/set-body! ":post-body")))

(defn example-servlet-handler
  [ctx]
  (srv/response ctx 200 "main-body"))

(deftest test-routes
  (with-handler [["/filtered" example-filter-handler example-servlet-handler]
                 ["/servlet" example-servlet-handler]]
    (let [ret (send "http://localhost:42000/filtered")]
      (is (= (:status ret) 200))
      (is (= (get-in ret [:headers "foo1" 0]) "bar1"))
      (is (= (get-in ret [:headers "foo2" 0]) "bar2"))
      (is (= (:body ret) "pre-body:main-body:post-body")))
    (let [ret (send "http://localhost:42000/servlet")]
      (is (= (:status ret) 200))
      (is (= (:body ret) "main-body")))))