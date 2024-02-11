(ns dev.onionpancakes.serval.jsonista
  (:require [dev.onionpancakes.serval.response.body
             :as response.body]
            [jsonista.core :as json]))

(defn json-writable
  ([value]
   (response.body/writable-to-writer value json/write-value))
  ([value object-mapper]
   (response.body/writable-to-writer value json/write-value object-mapper)))
