(ns dev.onionpancakes.serval.jetty.impl.ee10.servlet
  (:require [dev.onionpancakes.serval.jetty.impl.handlers :as handlers]
            [dev.onionpancakes.serval.jetty.impl.protocols :as p]
            [dev.onionpancakes.serval.jetty.impl.ee10.protocols :as ee10.p])
  (:import [org.eclipse.jetty.ee10.servlet ServletHolder ServletContextHandler]))

;; ServletHolder

(defn servlet-holder
  [config]
  (let [holder (ServletHolder.)]
    (when (contains? config :servlet)
      (.setServlet holder (p/as-servlet (:servlet config))))
    (when (contains? config :name)
      (.setName holder (:name config)))
    (when (contains? config :multipart-config)
      (-> (.getRegistration holder)
          (.setMultipartConfig (:multipart-config config))))
    holder))

(extend-protocol ee10.p/ServletHolder
  java.util.Map
  (as-servlet-holder [this]
    (servlet-holder this))
  ServletHolder
  (as-servlet-holder [this] this)
  Object
  (as-servlet-holder [this]
    (servlet-holder {:servlet this})))

(defn as-servlet-holder
  ^ServletHolder
  [this]
  (ee10.p/as-servlet-holder this))

;; ServletContextHandler

(defn servlet-context-handler
  [config]
  (let [sch (ServletContextHandler.)]
    (when (contains? config :servlets)
      (doseq [[path srv-holder] (p/as-servlet-context-paths (:servlets config))]
        (.addServlet sch (as-servlet-holder srv-holder) ^String path)))
    (when (contains? config :session-handler)
      (.setSessionHandler sch (:session-handler config)))
    (when (contains? config :error-handler)
      (.setErrorHandler sch (handlers/as-error-handler (:error-handler config))))
    (when (contains? config :gzip-handler)
      (.insertHandler sch (handlers/as-gzip-handler (:gzip-handler config))))
    sch))

(extend-protocol ee10.p/ServletContextHandler
  java.util.Map
  (as-servlet-context-handler [this]
    (servlet-context-handler this))
  Object
  (as-servlet-context-handler [this]
    (servlet-context-handler {:servlets this}))
  nil
  (as-servlet-context-handler [_] nil))

(defn as-servlet-context-handler
  ^ServletContextHandler
  [this]
  (ee10.p/as-servlet-context-handler this))

