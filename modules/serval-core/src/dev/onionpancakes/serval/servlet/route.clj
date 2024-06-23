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
  (^FilterRegistration add-filter-to-context [this ctx filter-name])
  (filter-dispatcher-types [this]))

(defprotocol RouteServlet
  (servlet-name [this url-pattern])
  (^ServletRegistration add-servlet-to-context [this ctx servlet-name]))

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

(defn add-route-filters-for-url-pattern
  [ctx url-pattern filters]
  (let [url-pattern-arr (into-array String [url-pattern])]
    (doseq [filter filters]
      (.. (add-filter-to-context filter ctx (filter-name filter url-pattern))
          (addMappingForUrlPatterns (filter-dispatcher-types filter) true url-pattern-arr))))
  ctx)

(defn add-route-filters-for-servlet-name
  [ctx url-pattern filters servlet-name]
  (let [servlet-name-arr (into-array String [servlet-name])]
    (doseq [filter filters]
      (.. (add-filter-to-context filter ctx (filter-name filter url-pattern))
          (addMappingForServletNames (filter-dispatcher-types filter) true servlet-name-arr))))
  ctx)

;; Servlet

(defn add-route-servlet
  [ctx url-pattern filters servlet]
  (let [srv-name (servlet-name servlet url-pattern)]
    (.. (add-servlet-to-context servlet ctx srv-name)
        (addMapping (into-array String [url-pattern])))
    (add-route-filters-for-servlet-name ctx url-pattern filters srv-name))
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
  [ctx routes]
  (reduce add-route ctx routes))

;; RouteFilter

(extend-protocol RouteFilter
  ;; Fn
  clojure.lang.Fn
  (filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter-to-context [this ^ServletContext ctx ^String filter-name]
    (.addFilter ctx filter-name (servlet/filter this)))
  (filter-dispatcher-types [_] nil)
  ;; Var
  clojure.lang.Var
  (filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter-to-context [this ^ServletContext ctx ^String filter-name]
    (.addFilter ctx filter-name (servlet/filter this)))
  (filter-dispatcher-types [_] nil)
  ;; Http method set
  clojure.lang.IPersistentSet
  (filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter-to-context [this ^ServletContext ctx ^String filter-name]
    (.addFilter ctx filter-name (servlet/http-method-filter this)))
  (filter-dispatcher-types [_] nil)
  ;; Filter
  Filter
  (filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter-to-context [this ^ServletContext ctx ^String filter-name]
    (.addFilter ctx filter-name this))
  (filter-dispatcher-types [_] nil))

;; RouteServlet

(extend-protocol RouteServlet
  ;; Fn
  clojure.lang.Fn
  (servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet-to-context [this ^ServletContext ctx ^String servlet-name]
    (.addServlet ctx servlet-name (servlet/servlet this)))
  ;; Var
  clojure.lang.Var
  (servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet-to-context [this ^ServletContext ctx ^String servlet-name]
    (.addServlet ctx servlet-name (servlet/servlet this)))
  ;; Servlet
  Servlet
  (servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet-to-context [this ^ServletContext ctx ^String servlet-name]
    (.addServlet ctx servlet-name this)))
