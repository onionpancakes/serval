(ns dev.onionpancakes.serval.jetty-test
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.hop.client :as hop]))

;; Server

(def default-port 42000)

(defn default-handler
  [_ _ response]
  (srv/send-error response 501 "Server handler is not set."))

(def default-config
  {:connectors [{:port default-port}]
   :handler    default-handler})

(defonce server
  (doto (srv.jetty/server)
    (srv.jetty/configure default-config)))

(defmacro with-config
  [config & body]
  `(try
     (srv.jetty/restart server ~config)
     ~@body
     (finally
       (srv.jetty/stop server)
       (srv.jetty/configure server default-config))))

(defmacro with-handler
  [handler & body]
  `(with-config {:handler ~handler} ~@body))

;; Client

(def ^:dynamic *uri*
  (str "http://localhost:" default-port))

(def ^:dynamic *body-handler*
  :string)

(defprotocol ClientRequest
  (request-with-defaults [this]))

(extend-protocol ClientRequest
  java.util.Map
  (request-with-defaults [this]
    (merge {:uri *uri*} this))
  Object
  (request-with-defaults [this] this)
  nil
  (request-with-defaults [_] {:uri *uri*}))

(defn send
  ([] (send nil))
  ([req] (send req *body-handler*))
  ([req body-handler] (hop/send (request-with-defaults req) body-handler)))
