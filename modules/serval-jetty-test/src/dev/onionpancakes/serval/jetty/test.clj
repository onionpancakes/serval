(ns dev.onionpancakes.serval.jetty.test
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.hop.client :as hop]))

;; Server

(def server-port 42000)

(defn handler-not-set
  [_ _ response]
  (srv/send-error response 501 "Server handler is not set."))

(def server-config
  {:connectors [{:port server-port}]
   :handler    handler-not-set})

(defonce server
  (srv.jetty/server server-config))

(defmacro with-config
  [config & body]
  `(try
     (srv.jetty/restart server ~config)
     ~@body
     (finally
       (srv.jetty/stop server)
       (srv.jetty/configure-server server server-config))))

(defmacro with-handler
  [handler & body]
  `(with-config {:handler ~handler} ~@body))

;; Client

(def ^:dynamic *uri*
  (str "http://localhost:" server-port))

(def ^:dynamic *body-handler*
  :string)

(defprotocol ClientRequest
  (request-with-defaults [this]))

(extend-protocol ClientRequest
  java.util.Map
  (request-with-defaults [this]
    (merge {:uri *uri*} this))
  Object
  (request-with-defaults [this]
    this)
  nil
  (request-with-defaults [this]
    {:uri *uri*}))

(defn send
  ([] (send nil))
  ([req] (send req *body-handler*))
  ([req body-handler] (hop/send (request-with-defaults req) body-handler)))
