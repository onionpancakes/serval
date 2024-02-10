(ns dev.onionpancakes.serval.transit
  (:require [dev.onionpancakes.serval.response.body
             :as response.body]
            [cognitect.transit :as transit])
  (:import [jakarta.servlet ServletResponse]))

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

(deftype TransitBody [value type options]
  response.body/WritableToOutputStream
  (write-to-output-stream [_ out]
    (-> (transit/writer out type options)
        (transit/write value)))
  response.body/Body
  (write-body-to-response [this response]
    (.write-to-output-stream this (.getOutputStream ^ServletResponse response))))

(defn transit-body
  ([value type]
   (TransitBody. value type nil))
  ([value type options]
   (TransitBody. value type options)))
