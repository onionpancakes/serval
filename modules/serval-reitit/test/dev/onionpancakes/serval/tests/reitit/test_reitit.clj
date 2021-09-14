(ns dev.onionpancakes.serval.tests.reitit.test-reitit
  (:require [dev.onionpancakes.serval.reitit :as sr]
            [reitit.core :as r]
            [clojure.test :refer [deftest is]])
  (:import [reitit.core Match]))

(def router-spec
  [["/"    {:GET {:name    :root
                  :handler #(assoc % :handled "root")}}]
   ["/foo" {:GET {:name    :foo
                  :handler #(assoc % :handled "foo")}}]])

(def router
  (r/router router-spec))

(deftest test-match-by-path
  ;; Default opts
  (let [ctx {:serval.service/request {:path "/"}}
        ret (sr/match-by-path ctx router)]
    (is (instance? Match (:serval.reitit/match ret)))
    (is (= :root (-> ret :serval.reitit/match :data :GET :name))))

  ;; Custom opts
  (let [ctx  {:serval.service/request {:other-path "/foo"}}
        opts {:path-key  [:serval.service/request :other-path]
              :match-key [:match-here]}
        ret  (sr/match-by-path ctx router opts)]
    (is (instance? Match (:match-here ret)))
    (is (= :foo (-> ret :match-here :data :GET :name)))))

(deftest test-handle-match-by-method
  ;; Default opts
  (let [ctx {:serval.service/request {:method :GET
                                      :path   "/"}}
        ret (sr/match-by-path ctx router)
        ret (sr/handle-match-by-method ret)]
    (is (= "root" (:handled ret))))

  ;; Custom opts
  (let [ctx  {:serval.service/request {:method     :GET
                                       :other-path "/foo"}}
        opts {:path-key  [:serval.service/request :other-path]
              :match-key [:match-here]}
        ret  (sr/match-by-path ctx router opts)
        ret  (sr/handle-match-by-method ret opts)]
    (is (= "foo" (:handled ret)))))
