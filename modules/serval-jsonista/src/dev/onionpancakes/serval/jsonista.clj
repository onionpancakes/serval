(ns dev.onionpancakes.serval.jsonista
  (:require [dev.onionpancakes.serval.service.body
             :as service.body]
            [jsonista.core :as json]))

(def default-object-mapper
  json/default-object-mapper)

(def keyword-keys-object-mapper
  json/keyword-keys-object-mapper)

;; Read

(defn read-json
  ([ctx]
   (read-json ctx nil))
  ([ctx {:keys [object-mapper value-key error-key]
         :or   {object-mapper default-object-mapper
                value-key     :serval.jsonista/value
                error-key     :serval.jsonista/error}}]
   (try
     (let [value (-> (:serval.context/request ctx)
                     (:body)
                     (json/read-value object-mapper))]
       (assoc ctx value-key value))
     (catch Throwable ex
       (assoc ctx error-key ex)))))

;; Write

(defrecord JsonValue [value object-mapper]
  service.body/Writable
  (write [_ out _]
    (json/write-value out value object-mapper)))

(defn json-value
  ([value]
   (JsonValue. value default-object-mapper))
  ([value object-mapper]
   (JsonValue. value object-mapper)))
