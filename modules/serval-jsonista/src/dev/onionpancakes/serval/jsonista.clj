(ns dev.onionpancakes.serval.jsonista
  (:require [jsonista.core :as j]))

(defn read-json
  ([ctx]
   (read-json ctx nil))
  ([ctx {:keys [to from object-mapper]
         :or   {from          [:serval.service/request :reader]
                to            [:serval.request/body]
                object-mapper j/default-object-mapper}}]
   (try
     (let [source (get-in ctx from)
           value  (j/read-value source object-mapper)]
       (assoc-in ctx to value))
     (catch com.fasterxml.jackson.core.JsonParseException ex
       (assoc ctx :serval.jsonista/error {:exception ex})))))

#_(defrecord Json [value]
  IResponseBody
  (write-body* [this resp]))
