(ns dev.onionpancakes.serval.servlet.route
  (:refer-clojure :exclude [filter])
  (:require [dev.onionpancakes.serval.servlet :as servlet])
  (:import [jakarta.servlet
            DispatcherType
            Filter
            FilterRegistration
            Servlet
            ServletContext
            ServletRegistration]))

(defprotocol RouteFilter
  (filter-name [this url-pattern])
  (filter [this])
  (filter-dispatcher-types [this]))

(defprotocol RouteServlet
  (servlet-name [this url-pattern])
  (servlet [this]))

;; Filters

(defn as-dispatcher-type
  {:tag DispatcherType}
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

(defn as-dispatcher-type-enum-set
  {:tag java.util.EnumSet}
  [types]
  (java.util.EnumSet/copyOf ^java.util.Collection (mapv as-dispatcher-type types)))

(defn register-filter
  {:tag FilterRegistration}
  [^ServletContext ctx ^String filter-name ^Filter filter]
  (if-some [reg (.getFilterRegistration ctx filter-name)]
    reg
    (.addFilter ctx filter-name filter)))

(defn add-route-filters-for-url-pattern
  [ctx url-pattern route-filters]
  (let [url-pattern-arr (into-array String [url-pattern])]
    (doseq [rf route-filters]
      (.. (register-filter ctx (filter-name rf url-pattern) (filter rf))
          (addMappingForUrlPatterns (filter-dispatcher-types rf) true url-pattern-arr))))
  ctx)

(defn add-route-filters-for-servlet-name
  [ctx url-pattern route-filters servlet-name]
  (let [servlet-name-arr (into-array String [servlet-name])]
    (doseq [rf route-filters]
      (.. (register-filter ctx (filter-name rf url-pattern) (filter rf))
          (addMappingForServletNames (filter-dispatcher-types rf) true servlet-name-arr))))
  ctx)

;; Servlet

(defn register-servlet
  {:tag ServletRegistration}
  [^ServletContext ctx ^String servlet-name ^Servlet servlet]
  (if-some [reg (.getServletRegistration ctx servlet-name)]
    reg
    (.addServlet ctx servlet-name servlet)))

(defn add-route-servlet
  [ctx url-pattern route-filters route-servlet]
  (let [srv-name (servlet-name route-servlet url-pattern)]
    (.. (register-servlet ctx srv-name (servlet route-servlet))
        (addMapping (into-array String [url-pattern])))
    (add-route-filters-for-servlet-name ctx url-pattern route-filters srv-name))
  ctx)

;; Route

(defn add-route
  [ctx route]
  {:pre [(vector? route)
         (>= (count route) 2)]}
  (let [url-pattern (first route)
        filters     (next (pop route))
        servlet     (peek route)]
    (if (some? servlet)
      (add-route-servlet ctx url-pattern filters servlet)
      (add-route-filters-for-url-pattern ctx url-pattern filters))))

(defn add-routes
  [servlet-ctx routes]
  (reduce add-route servlet-ctx routes))

;; RouteFilter

(extend-protocol RouteFilter
  ;; Fn
  clojure.lang.Fn
  (filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (filter [this]
    (servlet/filter this))
  (filter-dispatcher-types [this] nil)
  ;; Var
  clojure.lang.Var
  (filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (filter [this]
    (servlet/filter this))
  (filter-dispatcher-types [this] nil)
  ;; Http method set
  clojure.lang.IPersistentSet
  (filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (filter [this]
    (servlet/http-method-filter this))
  (filter-dispatcher-types [this] nil)
  ;; Filter
  Filter
  (filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (filter [this] this)
  (filter-dispatcher-types [this] nil))

;; RouteServlet

(extend-protocol RouteServlet
  ;; Fn
  clojure.lang.Fn
  (servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (servlet [this]
    (servlet/servlet this))
  ;; Var
  clojure.lang.Var
  (servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (servlet [this]
    (servlet/servlet this))
  ;; Servlet
  Servlet
  (servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (servlet [this] this))
