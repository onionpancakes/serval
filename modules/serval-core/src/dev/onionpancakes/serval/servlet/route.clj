(ns dev.onionpancakes.serval.servlet.route
  (:require [dev.onionpancakes.serval.impl.http.filter
             :as impl.http.filter]
            [dev.onionpancakes.serval.impl.http.servlet
             :as impl.http.servlet])
  (:import [java.util EnumSet]
           [jakarta.servlet
            DispatcherType
            ServletContext
            ServletRegistration FilterRegistration]))

;; Filter DispatcherType

(defn as-dispatcher-type
  ^DispatcherType
  [k]
  (case k
    :async   DispatcherType/ASYNC
    :error   DispatcherType/ERROR
    :forward DispatcherType/FORWARD
    :include DispatcherType/INCLUDE
    :request DispatcherType/REQUEST
    (if (instance? DispatcherType k)
      k
      (throw (ex-info "Neither valid keyword or DispatcherType." {:arg k})))))

(defn dispatcher-type-enum-set
  ^EnumSet
  [types]
  (doto (EnumSet/noneOf DispatcherType)
    (.addAll (mapv as-dispatcher-type types))))

;; Route

(defprotocol RouteServlet
  (get-servlet-name [this url-pattern])
  (add-servlet ^ServletRegistration [this servlet-ctx servlet-name]))

(defprotocol RouteFilter
  (get-filter-name [this url-pattern])
  (add-filter ^FilterRegistration [this servlet-ctx filter-name])
  (get-dispatch-types [this]))

(extend-protocol RouteServlet
  clojure.lang.Fn
  (get-servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet [this ^ServletContext servlet-ctx ^String servlet-name]
    (if-some [reg (.getServletRegistration servlet-ctx servlet-name)]
      reg
      (.addServlet servlet-ctx servlet-name (impl.http.servlet/servlet this))))
  clojure.lang.Var
  (get-servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet [this ^ServletContext servlet-ctx ^String servlet-name]
    (if-some [reg (.getServletRegistration servlet-ctx servlet-name)]
      reg
      (.addServlet servlet-ctx servlet-name (impl.http.servlet/servlet this)))))

(extend-protocol RouteFilter
  clojure.lang.Fn
  (get-filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter [this ^ServletContext servlet-ctx ^String filter-name]
    (if-some [reg (.getFilterRegistration servlet-ctx filter-name)]
      reg
      (.addFilter servlet-ctx filter-name (impl.http.filter/filter this))))
  (get-dispatch-types [this] nil)
  clojure.lang.Var
  (get-filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter [this ^ServletContext servlet-ctx ^String filter-name]
    (if-some [reg (.getFilterRegistration servlet-ctx filter-name)]
      reg
      (.addFilter servlet-ctx filter-name (impl.http.filter/filter this))))
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
