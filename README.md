# Serval

[![Run tests](https://github.com/onionpancakes/serval/actions/workflows/run_tests.yml/badge.svg)](https://github.com/onionpancakes/serval/actions/workflows/run_tests.yml)

Servlet oriented web framework for Clojure.

## Motivations

* Composable linear request processing flow, using transducer like *middleware*.
* Access to the latest servlet API developments with minimal maintenance.
* Extensible request handling via protocols.
* Easy to use asynchronous APIs for I/O and computation. (TBD)

# Status

Currently for my personal use. Future breaking changes possible.

# Deps

For **all** modules, add the following.

```clojure
{:deps
  {dev.onionpancakes/serval
    {:git/url   "https://github.com/onionpancakes/serval"
     :git/sha   "<GIT SHA>"}}}
```

To specify specific modules, use `:deps/root`.

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

# Usage

Require the namespaces.

```clojure
(require '[dev.onionpancakes.serval.core :as srv])
(require '[dev.onionpancakes.serval.jetty :as srv.jetty])
```

Write a handler.

```clojure
(defn my-handler
  [ctx]
  ;; Assoc the response into ctx map.
  (into ctx {:serval.response/status       200
             :serval.response/body         "Hello world!"
             :serval.response/context-type "text/plain"}))
```

Add the handler to server.

```clojure
(defonce server
  (srv.jetty/server {:connectors [{:protocol :http
                                   :port     3000}]
                     :handler    my-handler}))

(defn -main []
  (srv.jetty/start server))
```

Alternatively, create a `jakarta.servlet.Servlet` from handler. Use it in your favorite servlet container.

```clojure
(srv/http-servlet my-handler)
```

See example directory for complete solution.

# License

Released under the MIT license.