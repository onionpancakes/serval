(ns dev.onionpancakes.serval.transit
  (:require [dev.onionpancakes.serval.io.body :as io.body]
            [cognitect.transit :as transit])
  (:import [jakarta.servlet ServletResponse]))

;; Read

(defn read-transit
  ([ctx]
   (read-transit ctx nil))
  ([ctx {:keys [from to error type]
         :or   {from  [:serval.service/request :input-stream]
                to    [:serval.transit/value]
                error [:serval.transit/error]
                type  :json}}]
   (try
     (let [value (-> (get-in ctx from)
                     (transit/reader type)
                     (transit/read))]
       (assoc-in ctx to value))
     (catch Exception ex
       (assoc-in ctx error {:exception ex})))))

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
