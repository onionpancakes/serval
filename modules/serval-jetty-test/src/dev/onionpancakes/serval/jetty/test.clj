(ns dev.onionpancakes.serval.jetty.test
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.hop.client :as hop])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpResponse
            HttpRequest$Builder HttpRequest$BodyPublishers
            HttpResponse$BodyHandlers]))

;; Server

(def server-port 42000)

(def server-response
  {:serval.response/status 501
   :serval.response/body   (ex-info "Server handler not set." {})})

(def server-config
  {:connectors [{:port server-port}]
   :handler    (constantly server-response)})

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

(defmacro with-response
  [response & body]
  `(with-config {:handler (constantly ~response)} ~@body))

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



