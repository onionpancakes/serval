(ns dev.onionpancakes.serval.tests.mock.test-http-response
  (:require [dev.onionpancakes.serval.mock.http :as http]
            [dev.onionpancakes.serval.mock.io :as io]
            [clojure.test :refer [deftest is]])
  (:import [java.io ByteArrayInputStream]))

(deftest test-encoding
  (let [req  (http/http-servlet-request {} "")
        resp (http/http-servlet-response {:character-encoding "utf-8"} req)]
    (is (= (.getCharacterEncoding resp) "utf-8"))))

(deftest test-default-encoding
  (let [req  (http/http-servlet-request {} "")
        resp (http/http-servlet-response {} req)]
    (is (= (.getCharacterEncoding resp) "ISO-8859-1"))))

(deftest test-set-methods
  (let [req  (http/http-servlet-request {} "")
        resp (http/http-servlet-response {} req)]
    (.sendError resp 404 "Not found!")
    (.setStatus resp 200)
    (.addHeader resp "Foo" "Bar")
    (.addIntHeader resp "Baz" 1)
    (.setContentType resp "text/plain")
    (.setCharacterEncoding resp "utf-8")
    (let [dval @(:data resp)]
      (is (= dval {:send-error         [404 "Not found!"]
                   :status             200
                   :headers            {"Foo" ["Bar"], "Baz" [1]}
                   :content-type       "text/plain"
                   :character-encoding "utf-8"})))))

(deftest test-output-stream
  (let [req  (http/http-servlet-request {} "")
        resp (http/http-servlet-response {} req)
        out  (.getOutputStream resp)
        _    (spit out "Foobar")
        res  (slurp (.toByteArray (:output-stream resp)))]
    (is (= res "Foobar"))
    (is (thrown? IllegalStateException (.getWriter resp)))))

(deftest test-writer
  (let [req  (http/http-servlet-request {} "")
        resp (http/http-servlet-response {} req)
        wtr  (.getWriter resp)
        _    (spit wtr "Foobar")
        res  (slurp (.toByteArray (:output-stream resp)))]
    (is (= res "Foobar"))
    (is (thrown? IllegalStateException (.getOutputStream resp)))))

(deftest test-write-async
  (let [req  (http/http-servlet-request {} "")
        resp (http/http-servlet-response {} req)
        _    (.startAsync req)
        out  (.getOutputStream resp)
        src  (ByteArrayInputStream. (.getBytes "Foobar" "utf-8"))
        cf   (io/write-async! out src)
        _    (.get cf)
        res  (slurp (.toByteArray (:output-stream resp)))]
    (is (= res "Foobar"))))

(deftest test-write-async-not-started
  (let [req  (http/http-servlet-request {} "")
        resp (http/http-servlet-response {} req)
        out  (.getOutputStream resp)
        src  (ByteArrayInputStream. (.getBytes "Foobar" "utf-8"))]
    (is (thrown? IllegalStateException (io/write-async! out src)))))


(defn run-tests []
  (clojure.test/run-tests 'dev.onionpancakes.serval.tests.mock.test-http-response))
