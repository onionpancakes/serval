(ns dev.onionpancakes.serval.core.tests.test-servlet
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.servlet :as srv.servlet]
            [dev.onionpancakes.serval.jetty.test
             :refer [with-handler send]]
            [clojure.test :refer [deftest is]]))


