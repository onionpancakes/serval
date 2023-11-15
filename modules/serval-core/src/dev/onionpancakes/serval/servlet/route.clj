(ns dev.onionpancakes.serval.servlet.route
  (:require [dev.onionpancakes.serval.servlet :as srv.servlet]))

(defprotocol RouteServlet
  (get-servlet-name [this])
  (add-servlet [this servlet-ctx servlet-name]))

(defprotocol RouteFilter
  (get-filter-name [this])
  (add-filter [this servlet-ctx filter-name]))

(extend-protocol RouteServlet
  clojure.lang.AFunction
  (get-servlet-name [this]
    (str "serval.servlet.route:" (hash this)))
  (add-servlet [this servlet-ctx servlet-name]
    (if-some [reg (.getServletRegistration servlet-ctx servlet-name)]
      reg
      (.addServlet servlet-ctx servlet-name (srv.servlet/servlet this)))))

(extend-protocol RouteFilter
  clojure.lang.AFunction
  (get-filter-name [this]
    (str "serval.servlet.route:" (hash this)))
  (add-filter [this servlet-ctx filter-name]
    (if-some [reg (.getFilterRegistration servlet-ctx filter-name)]
      reg
      (.addFilter servlet-ctx filter-name (srv.servlet/filter this)))))

(defn add-route
  [servlet-ctx route]
  (let [url-pattern   (first route)
        route-servlet (peek route)
        servlet-name  (get-servlet-name route-servlet)]
    (.. (add-servlet route-servlet servlet-ctx servlet-name)
        (addMapping (into-array String [url-pattern])))
    (doseq [route-filter (next (pop route))]
      (let [filter-name (get-filter-name route-filter)]
        (.. (add-filter route-filter servlet-ctx filter-name)
            (addMappingForServletName nil true (into-array String [servlet-name])))))
    servlet-ctx))

(defn add-routes
  [servlet-ctx routes]
  (reduce add-route servlet-ctx routes))
