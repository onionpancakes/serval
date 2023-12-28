(ns dev.onionpancakes.serval.jetty.impl.ee10.servlet
  (:require [dev.onionpancakes.serval.servlet.route :as srv.servlet.route]
            [dev.onionpancakes.serval.jetty.impl.handlers :as impl.handlers]
            [dev.onionpancakes.serval.jetty.impl.ee10.protocols :as ee10.p])
  (:import [org.eclipse.jetty.ee10.servlet ErrorPageErrorHandler ServletContextHandler]))

;; ErrorPageHandler

(defn add-error-page
  [^ErrorPageErrorHandler handler error ^String location]
  (cond
    (int? error)    (.addErrorPage handler ^long error location)
    (vector? error) (.addErrorPage handler (nth error 0) (nth error 1) location)
    (class? error)  (.addErrorPage handler ^Class error location))
  handler)

(defn add-error-pages
  [^ErrorPageErrorHandler handler error-pages]
  (reduce-kv add-error-page handler error-pages))

(defn error-page-error-handler
  ^ErrorPageErrorHandler
  [config]
  (let [handler (ErrorPageErrorHandler.)]
    (when (contains? config :error-pages)
      (add-error-pages handler (:error-pages config)))
    handler))

;; ServletContextHandler

(defn servlet-context-handler
  ^ServletContextHandler
  [config]
  (let [handler (ServletContextHandler.)]
    (when (contains? config :display-name)
      (.setDisplayName handler (:display-name config)))
    (when (contains? config :context-path)
      (.setContextPath handler (:context-path config)))
    (when (contains? config :routes)
      (-> (.getServletContext handler)
          (srv.servlet.route/add-routes (:routes config))))
    (when (contains? config :error-pages)
      (let [error-pages   (:error-pages config)
            error-handler (error-page-error-handler {:error-pages error-pages})]
        (.setErrorHandler handler error-handler)))
    (when (contains? config :session-handler)
      (.setSessionHandler handler (:session-handler config)))
    (when (contains? config :gzip-handler)
      (let [gz-config (:gzip-handler config)]
        (cond
          (true? gz-config)  (.insertHandler handler (impl.handlers/gzip-handler nil))
          (false? gz-config) nil
          :else              (.insertHandler handler (impl.handlers/as-gzip-handler gz-config)))))
    handler))

(extend-protocol ee10.p/ServletContextHandler
  clojure.lang.IPersistentMap
  (as-servlet-context-handler [this]
    (servlet-context-handler this))
  Object
  (as-servlet-context-handler [this]
    (servlet-context-handler {:routes [["/*" this]]}))
  nil
  (as-servlet-context-handler [this]
    (servlet-context-handler this)))

(defn as-servlet-context-handler
  [this]
  (ee10.p/as-servlet-context-handler this))
