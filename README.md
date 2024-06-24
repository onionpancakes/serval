# Serval

Servlet oriented web framework for Clojure.


# Status

[![Run tests](https://github.com/onionpancakes/serval/actions/workflows/run_tests.yml/badge.svg)](https://github.com/onionpancakes/serval/actions/workflows/run_tests.yml)

Currently for my personal use. Future breaking changes possible.

# Deps

For **all** modules, add the following.

```clojure
{:deps
  {dev.onionpancakes/serval
    {:git/url   "https://github.com/onionpancakes/serval"
     :git/sha   "<GIT SHA>"}}}
```

For specific modules, use `:deps/root`.

```clojure
{:deps
  {dev.onionpancakes/serval-core
    {:git/url   "https://github.com/onionpancakes/serval"
     :git/sha   "<GIT SHA>"
     :deps/root "modules/serval-core"}
   dev.onionpancakes/serval-jetty
    {:git/url   "https://github.com/onionpancakes/serval"
     :git/sha   "<GIT SHA>"
     :deps/root "modules/serval-jetty"}}}
```

# Example

Require the namespaces.

```clojure
(require '[dev.onionpancakes.serval.core :as srv])
(require '[dev.onionpancakes.serval.jetty :as srv.jetty])
```

Write servlet service functions.

```clojure
(defn hello-world
  [_ _ response]
  (doto response
    (srv/set-http :status 200
                  :content-type "text/plain"
                  :character-encoding "utf-8")
    (srv/write-body "Hello world!")))

(defn not-found
  [_ _ response]
  (doto response
    (srv/set-http :status 404
                  :content-type "text/plain"
                  :character-encoding "utf-8")
    (srv/write-body "Not found.")))

(defn error
  [_ _ response]
  (doto response
    (srv/set-http :status 500
                  :content-type "text/plain"
                  :character-encoding "utf-8")
    (srv/write-body "Error happened.")))
```

In an app map, add the service fns to :routes and :error-pages.

```clojure
;; Note: :routes uses servlet url-pattern "routing"
;; e.g. ""       - match root
;;      "/*"     - match wildcard
;;      "/foo"   - match prefix
;;      "/foo/*" - match prefix with wildcard
;;      "*.css"  - match extension
;;      "/"      - match default
(def app
  {:routes      [["" hello-world]
                 ["/not-found" not-found]
                 ["/error" error]]
   :error-pages {404       "/not-found"
                 Throwable "/error"}})
```

Add the app to a server.

```clojure
(defonce server
  (srv.jetty/server))

(defn -main []
  (srv.jetty/configure server {:connectors [{:port 3000}]
                               :handler    app})
  (srv.jetty/start server))
```

See example directory for complete solution.

# License

Released under the MIT license.