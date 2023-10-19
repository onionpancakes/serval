(ns dev.onionpancakes.serval.transit
  (:require [dev.onionpancakes.serval.io.body :as io.body]
            [cognitect.transit :as transit])
  (:import [jakarta.servlet ServletResponse]))

;; Read

(defn read-transit
  ([ctx type]
   (read-transit ctx type nil))
  ([ctx type {:keys [reader-opts value-key error-key]
              :or   {value-key :serval.transit/value
                     error-key :serval.transit/error}}]
   (let [reader (-> (:serval.service/request ctx)
                    (:body)
                    (transit/reader type reader-opts))]
     (try
       (assoc ctx value-key (transit/read reader))
       (catch Throwable ex
         (assoc ctx error-key ex))))))

;; Write

(defrecord TransitBody [value type options]
  io.body/Writable
  (io.body/write [_ out _]
    (-> (transit/writer out type options)
        (transit/write value))))

(defn transit-body
  ([value type]
   (TransitBody. value type nil))
  ([value type options]
   (TransitBody. value type options)))

#_(defn extend-map-as-transit-response-body!
  ([type]
   (extend-map-as-transit-response-body! type nil))
  ([type options]
   (extend-type java.util.Map
     io.body/Writable
     (io.body/write [this out]
       (-> (transit/writer out type options)
           (transit/write this))))))
