(ns dev.onionpancakes.serval.jetty
  (:import [jakarta.servlet Servlet]
           [org.eclipse.jetty.server Server Handler]
           [org.eclipse.jetty.servlet
            ServletHolder ServletContextHandler]))

(defn servlet-context-handler
  [config]
  (let [^ServletContextHandler handler (ServletContextHandler.)]
    (doseq [[^String path ^Servlet servlet] (:servlets config)]
      (.addServlet handler (ServletHolder. servlet) path))
    handler))

(defn server
  [config]
  (doto (Server. ^Integer (:port config))
    (.setHandler (servlet-context-handler config))))
