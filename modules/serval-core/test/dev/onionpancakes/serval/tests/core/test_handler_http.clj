(ns dev.onionpancakes.serval.tests.core.test-handler-http
  (:require [dev.onionpancakes.serval.handlers.http :as h]
            [clojure.test :refer [deftest is]]))

(deftest test-response-handler
  (is (= (h/response nil 200 "Foo")
         {:serval.response/status 200
          :serval.response/body   "Foo"}))
  (is (= (h/response nil 200 "Foo" "text/plain")
         {:serval.response/status       200
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (h/response nil 200 "Foo" "text/plain" "utf-8")
         {:serval.response/status             200
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"}))
  (is (= (h/response {} 200 "Foo")
         {:serval.response/status 200
          :serval.response/body   "Foo"}))
  (is (= (h/response {:foo :bar} 200 "Foo")
         {:foo                    :bar
          :serval.response/status 200
          :serval.response/body   "Foo"}))
  (is (= (h/response {} 400 "Foo" "text/plain")
         {:serval.response/status       400
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (h/response {:foo :bar} 400 "Foo" "text/plain")
         {:foo                          :bar
          :serval.response/status       400
          :serval.response/body         "Foo"
          :serval.response/content-type "text/plain"}))
  (is (= (h/response {} 400 "Foo" "text/plain" "utf-8")
         {:serval.response/status             400
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"}))
  (is (= (h/response {:foo :bar} 400 "Foo" "text/plain" "utf-8")
         {:foo                                :bar
          :serval.response/status             400
          :serval.response/body               "Foo"
          :serval.response/content-type       "text/plain"
          :serval.response/character-encoding "utf-8"})))

(deftest test-set-headers
  (is (-> nil
          (h/set-headers)
          (= {:serval.response/headers {}})))
  (is (-> {}
          (h/set-headers :foo)
          (= {:serval.response/headers {}})))
  (is (-> {:foo :bar}
          (h/set-headers)
          (= {:foo                     :bar
              :serval.response/headers {}})))
  (is (-> {:foo :bar}
          (h/set-headers :abc "xyz")
          (= {:foo                     :bar
              :serval.response/headers {"abc" ["xyz"]}})))
  (is (-> {:serval.response/headers {"replaced"     ["foo"]
                                     "not-replaced" ["bar"]}}
          (h/set-headers :replaced "abc" :replaced "def")
          (= {:serval.response/headers {"replaced"     ["abc" "def"]
                                        "not-replaced" ["bar"]}}))))
