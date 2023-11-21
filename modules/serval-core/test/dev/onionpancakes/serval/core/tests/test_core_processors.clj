(ns dev.onionpancakes.serval.core.tests.test-core-processors
  (:require [dev.onionpancakes.serval.core :as srv]
            [clojure.test :refer [deftest are]]))

(deftest test-map-sum-args
  (are [args expected] (let [handler (srv/handler (apply srv/map + args))]
                         (= (handler 0) expected))
    []          0
    [1]         1
    [1 2]       3
    [1 2 3]     6
    [1 2 3 4]   10
    [1 2 3 4 5] 15))

(deftest test-terminate-sum-args
  (are [args expected] (let [pred    (constantly true)
                             handler (srv/handler (apply srv/terminate pred + args))]
                         (= (handler 0) expected))
    []          0
    [1]         1
    [1 2]       3
    [1 2 3]     6
    [1 2 3 4]   10
    [1 2 3 4 5] 15))

(deftest test-when-sum-args
  (are [args expected] (let [pred    (constantly true)
                             handler (srv/handler (apply srv/when pred + args))]
                         (= (handler 0) expected))
    []          0
    [1]         1
    [1 2]       3
    [1 2 3]     6
    [1 2 3 4]   10
    [1 2 3 4 5] 15))

(def example-map-identity
  (srv/handler (srv/map identity)))

(deftest test-map-identity
  (are [input expected] (= (example-map-identity input) expected)
    nil         nil
    :foo        :foo
    0           0
    {:foo :bar} {:foo :bar}))

(def example-terminate
  (srv/handler (comp (srv/terminate :terminate identity)
                     (srv/map assoc :foo :bar))))

(deftest test-terminate
  (are [input expected] (= (example-terminate input) expected)
    {}                 {:foo :bar}
    {:terminate true}  {:terminate true}
    {:terminate false} {:terminate false :foo :bar}
    nil                {:foo :bar}))

(def example-terminate-and-continue
  (srv/handler (comp (srv/terminate :terminate identity)
                     (srv/map assoc :foo :bar))
               (comp (srv/terminate :terminate2 identity)
                     (srv/map assoc :foo2 :bar2))))

(deftest test-terminate-and-continue
  (are [input expected] (= (example-terminate-and-continue input) expected)
    {}                                 {:foo :bar :foo2 :bar2}
    {:terminate true}                  {:terminate true :foo2 :bar2}
    {:terminate true :terminate2 true} {:terminate true :terminate2 true}))

(def example-when
  (srv/handler (comp (srv/when :when assoc :foo :bar)
                     (srv/map assoc :baz :buz))))

(deftest test-when
  (are [input expected] (= (example-when input) expected)
    {}           {:baz :buz}
    {:when true} {:when true :foo :bar :baz :buz}))
