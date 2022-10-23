(ns dev.onionpancakes.serval.jsonista
  (:require [dev.onionpancakes.serval.io.body :as io.body]
            [jsonista.core :as j])
  (:import [jakarta.servlet ServletResponse]))

(def default-object-mapper
  j/default-object-mapper)

(def keyword-keys-object-mapper
  j/keyword-keys-object-mapper)

;; Read

(defn read-json
  ([ctx]
   (read-json ctx nil))
  ([ctx {:keys [object-mapper value-key error-key]
         :or   {object-mapper default-object-mapper
                value-key     :serval.jsonista/value
                error-key     :serval.jsonista/error}}]
   (try
     (let [value (-> (:serval.service/request ctx)
                     (:input-stream)
                     (j/read-value object-mapper))]
       (assoc ctx value-key value))
     (catch Throwable ex
       (assoc ctx error-key ex)))))

;; Write

(defrecord JsonBody [value object-mapper]
  io.body/ResponseBodySync
  (io.body/service-body-sync [_ _ _ response]
    (-> (.getOutputStream ^ServletResponse response)
        (j/write-value value object-mapper))))

(defn json-body
  ([value]
   (JsonBody. value default-object-mapper))
  ([value object-mapper]
   (JsonBody. value object-mapper)))

#_(defn extend-map-as-json-response-body!
  ([]
   (extend-map-as-json-response-body! default-object-mapper))
  ([object-mapper]
   (extend-type java.util.Map
     io.body/ResponseBodySync
     (io.body/service-body-sync [this _ _ response]
       (-> (.getOutputStream ^ServletResponse response)
           (j/write-value this object-mapper))))))
