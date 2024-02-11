(ns dev.onionpancakes.serval.chassis
  (:require [dev.onionpancakes.serval.response.body
             :as response.body]
            [dev.onionpancakes.chassis.core :as html]))

(defn html-writable
  "Returns a response body writable instance for a Chassis HTML value."
  [value]
  (response.body/writable-to-writer value html/write-html))
