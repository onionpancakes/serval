(ns dev.onionpancakes.serval.tests.core.test-processor
  (:require [dev.onionpancakes.serval.core :as c]
            [clojure.test :refer [deftest are]]))

(def echo
  (c/handler (c/map identity)))

(deftest test-echo
  (are [input expected] (= (echo input) expected)
    nil         nil
    :foo        :foo
    0           0
    {:foo :bar} {:foo :bar}))

(def times2-plus1
  (c/handler (comp (c/map * 2)
                   (c/map + 1))))

(deftest test-times2-plus1
  (are [input expected] (= (times2-plus1 input) expected)
    0  1
    1  3
    2  5
    -1 -1))

(def terminate-on-key
  (c/handler (comp (c/terminate :stop assoc :aborted? true)
                   (c/map assoc :foo :bar))))

(deftest test-terminate-with-key
  (are [x y] (= (terminate-on-key x) y)
    {}            {:foo :bar}
    {:stop true}  {:stop true :aborted? true}
    {:stop false} {:stop false :foo :bar}
    nil           {:foo :bar}))

(def terminate-and-continue
  (c/handler (comp (c/terminate :stop identity)
                   (c/map assoc :foo :bar))
             (comp (c/terminate :stop2 identity)
                   (c/map assoc :foo2 :bar2))))

(deftest test-terminate-and-continue
  (are [x y] (= (terminate-and-continue x) y)
    {}                       {:foo  :bar :foo2 :bar2}
    {:stop true}             {:stop true :foo2 :bar2}
    {:stop true :stop2 true} {:stop true :stop2 true}))

(def test-when-example
  (c/handler (comp (c/map assoc :foo :bar)
                   (c/when :add assoc :added :data)
                   (c/map assoc :baz :buz))))

(deftest test-when
  (are [x y] (= (test-when-example x) y)
    {}          {:foo :bar :baz :buz}
    {:abc 123}  {:foo :bar :baz :buz :abc 123}
    {:add true} {:foo :bar :baz :buz :added :data :add true}))
