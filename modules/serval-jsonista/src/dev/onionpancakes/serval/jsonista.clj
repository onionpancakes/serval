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
   (read-json ctx default-object-mapper))
  ([ctx object-mapper]
   (try
     (let [value (-> (:serval.service/request ctx)
                     (:input-stream)
                     (j/read-value object-mapper))]
       (assoc ctx :serval.jsonista/value value))
     (catch Throwable ex
       (assoc ctx :serval.jsonista/error ex)))))

;; Write

(defrecord JsonBody [value object-mapper]
  io.body/ResponseBody
  (io.body/service-body [_ _ _ response]
    (-> (.getOutputStream ^ServletResponse response)
        (j/write-value value object-mapper))))

(defn json-body
  ([value]
   (JsonBody. value default-object-mapper))
  ([value object-mapper]
   (JsonBody. value object-mapper)))

(defn extend-map-as-json-response-body!
  ([]
   (extend-map-as-json-response-body! default-object-mapper))
  ([object-mapper]
   (extend-protocol io.body/ResponseBody
     java.util.Map
     (io.body/service-body [this _ _ response]
       (-> (.getOutputStream ^ServletResponse response)
           (j/write-value this object-mapper))))))
