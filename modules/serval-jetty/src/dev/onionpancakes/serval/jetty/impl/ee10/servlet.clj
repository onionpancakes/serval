(ns dev.onionpancakes.serval.jetty.impl.ee10.servlet
  (:require [dev.onionpancakes.serval.servlet.route :as srv.servlet.route]
            [dev.onionpancakes.serval.jetty.impl.handlers :as handlers]
            [dev.onionpancakes.serval.jetty.impl.ee10.protocols :as ee10.p])
  (:import [org.eclipse.jetty.ee10.servlet ServletContextHandler]))

;; ServletContextHandler

(defn servlet-context-handler
  ^ServletContextHandler
  [config]
  (let [sch (ServletContextHandler.)]
    (when (contains? config :routes)
      (-> (.getServletContext sch)
          (srv.servlet.route/add-routes (:routes config))))
    (when (contains? config :session-handler)
      (.setSessionHandler sch (:session-handler config)))
    (when (contains? config :error-handler)
      (.setErrorHandler sch (handlers/as-error-handler (:error-handler config))))
    (when (contains? config :gzip-handler)
      (.insertHandler sch (handlers/as-gzip-handler (:gzip-handler config))))
    sch))

(extend-protocol ee10.p/ServletContextHandler
  clojure.lang.APersistentMap
  (as-servlet-context-handler [this]
    (servlet-context-handler this))
  clojure.lang.APersistentVector
  (as-servlet-context-handler [this]
    (servlet-context-handler {:routes this}))
  Object
  (as-servlet-context-handler [this]
    (servlet-context-handler {:routes [["/*" this]]}))
  nil
  (as-servlet-context-handler [this] nil))

(defn as-servlet-context-handler
  [this]
  (ee10.p/as-servlet-context-handler this))
