(ns dev.onionpancakes.serval.transit
  (:require [dev.onionpancakes.serval.io.body :as io.body]
            [cognitect.transit :as transit])
  (:import [jakarta.servlet ServletResponse]))

;; Read

(defn read-transit
  ([ctx type]
   (read-transit ctx type nil))
  ([ctx type {:keys [reader-opts]}]
   (try
     (let [value (-> (:serval.service/request ctx)
                     (:input-stream)
                     (transit/reader type reader-opts)
                     (transit/read))]
       (assoc ctx :serval.transit/value value))
     (catch Throwable ex
       (assoc ctx :serval.transit/error ex)))))

;; Write

(defrecord TransitBody [value type options]
  io.body/ResponseBody
  (io.body/service-body [_ _ _ response]
    (-> (.getOutputStream ^ServletResponse response)
        (transit/writer type options)
        (transit/write value))))

(defn transit-body
  ([value type]
   (TransitBody. value type nil))
  ([value type options]
   (TransitBody. value type options)))

(defn extend-map-as-transit-response-body!
  ([type]
   (extend-map-as-transit-response-body! type nil))
  ([type options]
   (extend-protocol io.body/ResponseBody
     java.util.Map
     (io.body/response-body [this _ _ response]
       (-> (.getOutputStream ^ServletResponse response)
           (transit/writer type options)
           (transit/write this))))))
