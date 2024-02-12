(ns dev.onionpancakes.serval.core.tests.test-core-handlers-bang
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is]]))

(deftest test-set-headers!
  (with-handler #(srv/set-headers! % {"foo" "bar"
                                      "baz" [1 2 3]})
    (let [ret-headers (:headers (send))]
      (is (= (get-in ret-headers ["foo" 0]) "bar"))
      (is (= (get-in ret-headers ["baz"]) ["1" "2" "3"])))))

(deftest test-set-body!
  (with-handler #(srv/set-body! % "foo")
    (let [ret (send)]
      (is (= (:body ret) "foo"))))
  (with-handler #(srv/set-body! % "foo" "text/plain")
    (let [ret (send)]
      (is (= (:body ret) "foo"))
      (is (= (:media-type ret) "text/plain"))))
  (with-handler #(srv/set-body! % "foo" "text/plain" "utf-8")
    (let [ret (send)]
      (is (= (:body ret) "foo"))
      (is (= (:media-type ret) "text/plain"))
      (is (= (:character-encoding ret) "utf-8"))
      (is (= (:content-type ret) "text/plain;charset=utf-8"))))

  ;; Resetting
  (with-handler #(-> (srv/write-body! % "foo")
                     (srv/set-body! "bar"))
    (let [ret (send)]
      (is (= (:body ret) "bar"))))
  (with-handler #(-> (srv/write-body! % "foo" "text/plain")
                     (srv/set-body! "bar" "text/html"))
    (let [ret (send)]
      (is (= (:body ret) "bar"))
      (is (= (:media-type ret) "text/html"))))
  (with-handler #(-> (srv/write-body! % "foo" "text/plain" "utf-8")
                     (srv/set-body! "bar" "text/html" "utf-16"))
    (let [ret (send)]
      (is (= (:body ret) "bar"))
      (is (= (:media-type ret) "text/html"))
      ;; Can't change character encoding after writing.
      (is (= (:character-encoding ret) "utf-8"))
      (is (= (:content-type ret) "text/html;charset=utf-8")))))

(deftest test-write-body!
  (with-handler #(srv/write-body! % "foo")
    (let [ret (send)]
      (is (= (:body ret) "foo"))))
  (with-handler #(srv/write-body! % "foo" "text/plain")
    (let [ret (send)]
      (is (= (:body ret) "foo"))
      (is (= (:media-type ret) "text/plain"))))
  (with-handler #(srv/write-body! % "foo" "text/plain" "utf-8")
    (let [ret (send)]
      (is (= (:body ret) "foo"))
      (is (= (:media-type ret) "text/plain"))
      (is (= (:character-encoding ret) "utf-8"))
      (is (= (:content-type ret) "text/plain;charset=utf-8")))))

(deftest test-set-response!
  (with-handler #(srv/set-response! % 200)
    (let [ret (send)]
      (is (= (:status ret) 200))))
  (with-handler #(srv/set-response! % 200 "foo")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "foo"))))
  (with-handler #(srv/set-response! % 200 "foo" "text/plain")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "foo"))
      (is (= (:media-type ret) "text/plain"))))
  (with-handler #(srv/set-response! % 200 "foo" "text/plain" "utf-8")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "foo"))
      (is (= (:media-type ret) "text/plain"))
      (is (= (:character-encoding ret) "utf-8"))
      (is (= (:content-type ret) "text/plain;charset=utf-8")))))

(deftest test-set-response-kv!
  (with-handler #(srv/set-response-kv! %
                                       :serval.response/status 200
                                       :serval.response/headers {"foo" "bar"}
                                       {:serval.response/body               "foo"
                                        :serval.response/content-type       "text/plain"
                                        :serval.response/character-encoding "utf-8"})
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (get-in ret [:headers "foo" 0]) "bar"))
      (is (= (:body ret) "foo"))
      (is (= (:content-type ret) "text/plain;charset=utf-8"))
      (is (= (:character-encoding ret) "utf-8")))))

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

(deftest test-do-filter-chain!
  (with-handler {:routes [["/*" example-filter-handler example-servlet-handler]]}
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (get-in ret [:headers "foo1" 0]) "bar1"))
      (is (= (get-in ret [:headers "foo2" 0]) "bar2"))
      (is (= (:body ret) "pre-body:main-body:post-body")))))
