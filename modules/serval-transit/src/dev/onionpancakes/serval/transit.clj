(ns dev.onionpancakes.serval.transit
  (:require [dev.onionpancakes.serval.response.body
             :as response.body]
            [cognitect.transit :as transit]))

(defn write-transit
  "Writes transit value to an OutputStream."
  ([out value type]
   (-> (transit/writer out type)
       (transit/write value)))
  ([out value type opts]
   (-> (transit/writer out type opts)
       (transit/write value))))

(defn transit-writable
  "Returns a response body writable instance for a transit value."
  ([value type]
   (response.body/writable-to-output-stream value write-transit type))
  ([value type opts]
   (response.body/writable-to-output-stream value write-transit type opts)))
