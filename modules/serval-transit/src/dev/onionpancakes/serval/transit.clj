(ns dev.onionpancakes.serval.transit
  (:require [dev.onionpancakes.serval.service.body
             :as service.body]
            [cognitect.transit :as transit]))

;; Read

(defn read-transit
  ([ctx type]
   (read-transit ctx type nil))
  ([ctx type {:keys [reader-opts value-key error-key]
              :or   {value-key :serval.transit/value
                     error-key :serval.transit/error}}]
   (let [reader (-> (:serval.context/request ctx)
                    (:body)
                    (transit/reader type reader-opts))]
     (try
       (assoc ctx value-key (transit/read reader))
       (catch Throwable ex
         (assoc ctx error-key ex))))))

;; Write

(defrecord TransitValue [value type options]
  service.body/Writable
  (write [_ out _]
    (-> (transit/writer out type options)
        (transit/write value))))

(defn transit-value
  ([value type]
   (TransitValue. value type nil))
  ([value type options]
   (TransitValue. value type options)))
