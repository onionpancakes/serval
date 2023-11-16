(ns dev.onionpancakes.serval.servlet.route
  (:require [dev.onionpancakes.serval.servlet :as srv.servlet]))

(defprotocol RouteServlet
  (get-servlet-name [this url-pattern])
  (add-servlet [this servlet-ctx servlet-name]))

(defprotocol RouteFilter
  (get-filter-name [this url-pattern])
  (add-filter [this servlet-ctx filter-name])
  (get-dispatch-types [this]))

(extend-protocol RouteServlet
  clojure.lang.AFunction
  (get-servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet [this servlet-ctx servlet-name]
    (if-some [reg (.getServletRegistration servlet-ctx servlet-name)]
      reg
      (.addServlet servlet-ctx servlet-name (srv.servlet/servlet this)))))

(extend-protocol RouteFilter
  clojure.lang.AFunction
  (get-filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter [this servlet-ctx filter-name]
    (if-some [reg (.getFilterRegistration servlet-ctx filter-name)]
      reg
      (.addFilter servlet-ctx filter-name (srv.servlet/filter this))))
  (get-dispatch-types [this] nil))

(defn add-route
  [servlet-ctx route]
  {:pre [(vector? route)
         (>= (count route) 2)]}
  (let [url-pattern   (first route)
        route-servlet (peek route)
        route-filters (next (pop route))
        servlet-name  (get-servlet-name route-servlet url-pattern)]
    ;; Add servlet
    (.. (add-servlet route-servlet servlet-ctx servlet-name)
        (addMapping (into-array String [url-pattern])))
    ;; Add filter
    (doseq [route-filter route-filters]
      (let [filter-name    (get-filter-name route-filter url-pattern)
            dispatch-types (get-dispatch-types route-filter)]
        (.. (add-filter route-filter servlet-ctx filter-name)
            (addMappingForServletNames dispatch-types true (into-array String [servlet-name])))))
    servlet-ctx))

(defn add-routes
  [servlet-ctx routes]
  (reduce add-route servlet-ctx routes))
