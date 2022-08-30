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

(defrecord TransitBody [value options]
  io.body/ResponseBody
  (io.body/service-body [_ _ _ response]
    (let [write-type (:type options :json)
          writer     (-> (.getOutputStream ^ServletResponse response)
                         (transit/writer write-type))]
      (transit/write writer value))))

(defn transit-body
  ([value]
   (TransitBody. value nil))
  ([value options]
   (TransitBody. value options)))

(defn extend-map-as-transit-response-body!
  ([]
   (extend-map-as-transit-response-body! nil))
  ([options]
   (extend-protocol io.body/ResponseBody
     java.util.Map
     (io.body/response-body [this _ _ response]
       (let [write-type (:type options :json)
             writer     (-> (.getOutputStream ^ServletResponse response)
                            (transit/writer write-type))]
         (transit/write writer this))))))
