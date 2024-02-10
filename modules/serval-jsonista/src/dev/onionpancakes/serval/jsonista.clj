(ns dev.onionpancakes.serval.jsonista
  (:require [dev.onionpancakes.serval.response.body
             :as response.body]
            [jsonista.core :as json])
  (:import [jakarta.servlet ServletRequest ServletResponse]))

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
     (let [value (-> ^ServletRequest (:serval.context/request ctx)
                     (.getReader)
                     (json/read-value object-mapper))]
       (assoc ctx value-key value))
     (catch Throwable ex
       (assoc ctx error-key ex)))))

;; Write

(deftype JsonBody [value object-mapper]
  response.body/WritableToWriter
  (write-to-writer [_ writer]
    (json/write-value writer value object-mapper))
  response.body/Body
  (write-body-to-response [this response]
    (.write-to-writer this (.getWriter ^ServletResponse response))))

(defn json-body
  ([value]
   (JsonBody. value default-object-mapper))
  ([value object-mapper]
   (JsonBody. value object-mapper)))
