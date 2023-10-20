(ns dev.onionpancakes.serval.tests.reitit.test-reitit
  (:require [dev.onionpancakes.serval.reitit :as srv.reitit]
            [clojure.test :refer [deftest is]])
  (:import [reitit.core Match]))

(def router-spec
  [["/get-method" {:GET {:handler #(assoc % :handled :get-method)}}]
   ["/no-method" {:handler #(assoc % :handled :no-method)}]
   ["/empty" {}]
   ["/" {:GET {:handler #(assoc % :handled :root)}}]])

(def router
  (srv.reitit/router router-spec))

(deftest test-route
  (let [ctx   {:serval.service/request {:method    :GET
                                        :path-info "/get-method"}}
        ret   (srv.reitit/route ctx router)
        match (:serval.reitit/match ret)]
    (is (instance? Match match))
    (is (= (get ret :handled) :get-method)))

  (let [ctx   {:serval.service/request {:method    :GET
                                        :path-info "/no-method"}}
        ret   (srv.reitit/route ctx router)
        match (:serval.reitit/match ret)]
    (is (instance? Match match))
    (is (= (get ret :handled) :no-method)))

  (let [ctx   {:serval.service/request {:method    :GET
                                        :path-info "/empty"}}
        ret   (srv.reitit/route ctx router)
        match (:serval.reitit/match ret)]
    (is (instance? Match match))
    (is (= (dissoc ret :serval.reitit/match) ctx)))

  (let [ctx {:serval.service/request {:method    :GET
                                      :path-info "/default"}}
        ret (srv.reitit/route ctx router {:default #(assoc % :handled :default)})]
    (is (= (get ret :handled) :default)))

  (let [ctx   {:serval.service/request {:method    :GET
                                        :path-info "/"}}
        ret   (srv.reitit/route ctx router)
        match (:serval.reitit/match ret)]
    (is (instance? Match match))
    (is (= (get ret :handled) :root)))

  (let [ctx   {:serval.service/request {:method :GET}}
        ret   (srv.reitit/route ctx router)
        match (:serval.reitit/match ret)]
    (is (instance? Match match))
    (is (= (get ret :handled) :root))))
