(ns dev.onionpancakes.serval.servlet.route
  (:require [dev.onionpancakes.serval.servlet :as servlet])
  (:import [java.util EnumSet]
           [jakarta.servlet
            Filter
            FilterRegistration
            Servlet
            ServletContext
            ServletRegistration]))

(defprotocol RouteFilter
  (get-filter-name [this url-pattern])
  (^FilterRegistration add-filter [this servlet-ctx filter-name])
  (get-dispatch-types [this]))

(defprotocol RouteServlet
  (get-servlet-name [this url-pattern])
  (^ServletRegistration add-servlet [this servlet-ctx servlet-name]))

;; Route

(defn add-filter-for-servlet-names
  [servlet-ctx filter-name filter servlet-names]
  (let [dispatch-types   (get-dispatch-types filter)
        servlet-name-arr (into-array String servlet-names)]
    (.. (add-filter filter servlet-ctx filter-name)
        (addMappingForServletNames dispatch-types true servlet-name-arr))))

(defn add-filter-for-url-patterns
  [servlet-ctx filter-name filter url-patterns]
  (let [dispatch-types  (get-dispatch-types filter)
        url-pattern-arr (into-array String url-patterns)]
    (.. (add-filter filter servlet-ctx filter-name)
        (addMappingForUrlPatterns dispatch-types true url-pattern-arr))))

(defn add-route-servlet
  [servlet-ctx url-pattern servlet filters]
  (let [servlet-name  (get-servlet-name servlet url-pattern)
        servlet-names [servlet-name]]
    (.. (add-servlet servlet servlet-ctx servlet-name)
        (addMapping (into-array String [url-pattern])))
    (doseq [filter filters
            :let   [filter-name (get-filter-name filter url-pattern)]]
      (add-filter-for-servlet-names servlet-ctx filter-name filter servlet-names))
    servlet-ctx))

(defn add-route-filters
  [servlet-ctx url-pattern filters]
  (let [url-patterns [url-pattern]]
    (doseq [filter filters
            :let   [filter-name (get-filter-name filter url-pattern)]]
      (add-filter-for-url-patterns servlet-ctx filter-name filter url-patterns))
    servlet-ctx))

(defn add-route
  [servlet-ctx route]
  {:pre [(vector? route)
         (>= (count route) 2)]}
  (let [url-pattern (first route)
        filters     (next (pop route))
        servlet     (peek route)]
    (if (some? servlet)
      (add-route-servlet servlet-ctx url-pattern servlet filters)
      (add-route-filters servlet-ctx url-pattern filters))))

(defn add-routes
  [servlet-ctx routes]
  (reduce add-route servlet-ctx routes))

;; RouteFilter

(extend-protocol RouteFilter
  ;; Fn
  clojure.lang.Fn
  (get-filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter [this ^ServletContext servlet-ctx ^String filter-name]
    (if-some [reg (.getFilterRegistration servlet-ctx filter-name)]
      reg
      (.addFilter servlet-ctx filter-name (servlet/filter this))))
  (get-dispatch-types [this] nil)
  ;; Var
  clojure.lang.Var
  (get-filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter [this ^ServletContext servlet-ctx ^String filter-name]
    (if-some [reg (.getFilterRegistration servlet-ctx filter-name)]
      reg
      (.addFilter servlet-ctx filter-name (servlet/filter this))))
  (get-dispatch-types [this] nil)
  ;; Http method set
  clojure.lang.IPersistentSet
  (get-filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter [this ^ServletContext servlet-ctx ^String filter-name]
    (if-some [reg (.getFilterRegistration servlet-ctx filter-name)]
      reg
      (.addFilter servlet-ctx filter-name (servlet/http-method-filter this))))
  (get-dispatch-types [this] nil)
  ;; Filter
  Filter
  (get-filter-name [this url-pattern]
    (str "serval.servlet.route/filter:" (hash this) ":" url-pattern))
  (add-filter [this ^ServletContext servlet-ctx ^String filter-name]
    (if-some [reg (.getFilterRegistration servlet-ctx filter-name)]
      reg
      (.addFilter servlet-ctx filter-name this)))
  (get-dispatch-types [this] nil))

;; RouteServlet

(extend-protocol RouteServlet
  ;; Fn
  clojure.lang.Fn
  (get-servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet [this ^ServletContext servlet-ctx ^String servlet-name]
    (if-some [reg (.getServletRegistration servlet-ctx servlet-name)]
      reg
      (.addServlet servlet-ctx servlet-name (servlet/servlet this))))
  ;; Var
  clojure.lang.Var
  (get-servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet [this ^ServletContext servlet-ctx ^String servlet-name]
    (if-some [reg (.getServletRegistration servlet-ctx servlet-name)]
      reg
      (.addServlet servlet-ctx servlet-name (servlet/servlet this))))
  ;; Servlet
  Servlet
  (get-servlet-name [this url-pattern]
    (str "serval.servlet.route/servlet:" (hash this) ":" url-pattern))
  (add-servlet [this ^ServletContext servlet-ctx ^String servlet-name]
    (if-some [reg (.getServletRegistration servlet-ctx servlet-name)]
      reg
      (.addServlet servlet-ctx servlet-name this))))
