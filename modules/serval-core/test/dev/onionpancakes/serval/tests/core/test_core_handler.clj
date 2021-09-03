(ns dev.onionpancakes.serval.tests.core.test-core-handler
  (:require [dev.onionpancakes.serval.core :as c]
            [clojure.test :refer [deftest is are]]))

(def identity-handler
  (c/handler (c/map identity)))

(deftest test-identity-handler
  (are [x y] (= (identity-handler x) y)
    nil         nil
    :foo        :foo
    0           0
    {:foo :bar} {:foo :bar}))

(def plus1-times2-handler
  (c/handler (comp (c/map inc)
                   (c/map * 2))))

(deftest test-plus1-times2-handler
  (are [x y] (= (plus1-times2-handler x) y)
    0  2
    1  4
    -1 0))

(def terminate-on-key-handler
  (c/handler (comp (c/terminate :stop identity)
                   (c/map assoc :foo :bar))))

(deftest test-terminate-with-key-handler
  (are [x y] (= (terminate-on-key-handler x) y)
    {}            {:foo :bar}
    {:stop true}  {:stop true}
    {:stop false} {:stop false :foo :bar}
    nil           {:foo :bar}))

(def terminate-and-continue-handler
  (c/handler (comp (c/terminate :stop identity)
                   (c/map assoc :foo :bar))
             (comp (c/terminate :stop2 identity)
                   (c/map assoc :foo2 :bar2))))

(deftest test-terminate-and-continue-handler
  (are [x y] (= (terminate-and-continue-handler x) y)
    {}                       {:foo  :bar :foo2 :bar2}
    {:stop true}             {:stop true :foo2 :bar2}
    {:stop true :stop2 true} {:stop true :stop2 true}))

