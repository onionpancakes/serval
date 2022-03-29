(ns user
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.jetty :as srv.jetty]
            [dev.onionpancakes.serval.reitit :as srv.reitit]
            [dev.onionpancakes.serval.jsonista :as srv.json]
            [promesa.core :as p]
            [clojure.pprint :refer [pprint]]))

(set! *warn-on-reflection* true)
