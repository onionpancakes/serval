(ns dev.onionpancakes.serval.jetty.impl.ee10.servlet
  (:require [dev.onionpancakes.serval.servlet.route :as srv.servlet.route])
  (:import [org.eclipse.jetty.ee10.servlet ErrorPageErrorHandler ServletContextHandler]))

;; ErrorPageHandler

(defn add-error-page
  [^ErrorPageErrorHandler handler error ^String location]
  (cond
    (int? error)    (.addErrorPage handler ^long error location)
    ;; Error range vector: [from to]
    (vector? error) (.addErrorPage handler (nth error 0) (nth error 1) location)
    (class? error)  (.addErrorPage handler ^Class error location))
  handler)

(defn add-error-pages
  [handler error-pages]
  (reduce-kv add-error-page handler error-pages))

(defn error-page-error-handler
  {:tag ErrorPageErrorHandler}
  [error-pages]
  (doto (ErrorPageErrorHandler.)
    (add-error-pages error-pages)))

;; ServletContextHandler

(defn servlet-context-handler
  {:tag ServletContextHandler}
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
      (->> (:error-pages config)
           (error-page-error-handler)
           (.setErrorHandler handler)))
    (when (contains? config :session-handler)
      (.setSessionHandler handler (:session-handler config)))
    (when (contains? config :handlers)
      (doseq [h (:handlers config)]
        (.insertHandler handler h)))
    handler))

(defn as-servlet-context-handler
  {:tag ServletContextHandler}
  [this]
  (if (map? this)
    (servlet-context-handler this)
    (servlet-context-handler {:routes [["/*" this]]})))
