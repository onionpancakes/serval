(ns dev.onionpancakes.serval.jsonista
  (:require [jsonista.core :as j]))

(defn read-json
  ([ctx]
   (read-json ctx nil))
  ([ctx {:keys [to from object-mapper]
         :or   {from          [:serval.service/request :reader]
                to            [:serval.request/body]
                object-mapper j/default-object-mapper}}]
   (let [source (get-in ctx from)
         value  (j/read-value source object-mapper)]
     (assoc-in ctx to value))))

#_(defrecord Json [value]
  IResponseBody
  (write-body* [this resp]))
