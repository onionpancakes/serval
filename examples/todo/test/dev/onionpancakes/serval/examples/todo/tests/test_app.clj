(ns dev.onionpancakes.serval.examples.todo.tests.test-app
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.examples.todo.app :as app]
            [dev.onionpancakes.serval.jetty-test
             :refer [with-handler send *uri*]]
            [clojure.test :refer [deftest is]]))

(deftest test-app
  (with-handler app/app
    (let [resp (send)]
      (is (= (:status resp) 200))
      (is (= (:media-type resp) "text/html"))
      (is (= (:character-encoding resp) "utf-8")))
    (let [resp (send (str *uri* "/not-found"))]
      (is (= (:status resp) 404))
      (is (= (:media-type resp) "text/html"))
      (is (= (:character-encoding resp) "utf-8")))
    (let [resp (send (str *uri* "/error"))]
      (is (= (:status resp) 500))
      (is (= (:media-type resp) "text/html"))
      (is (= (:character-encoding resp) "utf-8")))))
