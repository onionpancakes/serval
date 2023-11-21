(ns dev.onionpancakes.serval.core.tests.test-servlet
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.servlet :as srv.servlet]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is]]))

(defn args-handler
  ([ctx]
   (srv/response ctx 200 (list)))
  ([ctx a]
   (srv/response ctx 200 (list a)))
  ([ctx a b]
   (srv/response ctx 200 (list a b)))
  ([ctx a b c]
   (srv/response ctx 200 (list a b c)))
  ([ctx a b c d]
   (srv/response ctx 200 (list a b c d)))
  ([ctx a b c d & args]
   (srv/response ctx 200 (list* a b c d args))))

(deftest test-servlet
  (with-handler (srv.servlet/servlet args-handler)
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) ""))))
  (with-handler (srv.servlet/servlet args-handler "1")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "1"))))
  (with-handler (srv.servlet/servlet args-handler "1" "2")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "12"))))
  (with-handler (srv.servlet/servlet args-handler "1" "2" "3")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "123"))))
  (with-handler (srv.servlet/servlet args-handler "1" "2" "3" "4")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "1234"))))
  (with-handler (srv.servlet/servlet args-handler "1" "2" "3" "4" "5")
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "12345")))))

(deftest test-filter
  (with-handler [["/*"
                  (srv.servlet/filter args-handler)
                  (srv.servlet/servlet srv/response 400 "foo")]]
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) ""))))
  (with-handler [["/*"
                  (srv.servlet/filter args-handler "1")
                  (srv.servlet/servlet srv/response 400 "foo")]]
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "1"))))
  (with-handler [["/*"
                  (srv.servlet/filter args-handler "1" "2")
                  (srv.servlet/servlet srv/response 400 "foo")]]
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "12"))))
  (with-handler [["/*"
                  (srv.servlet/filter args-handler "1" "2" "3")
                  (srv.servlet/servlet srv/response 400 "foo")]]
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "123"))))
  (with-handler [["/*"
                  (srv.servlet/filter args-handler "1" "2" "3" "4")
                  (srv.servlet/servlet srv/response 400 "foo")]]
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "1234"))))
  (with-handler [["/*"
                  (srv.servlet/filter args-handler "1" "2" "3" "4" "5")
                  (srv.servlet/servlet srv/response 400 "foo")]]
    (let [ret (send)]
      (is (= (:status ret) 200))
      (is (= (:body ret) "12345")))))
