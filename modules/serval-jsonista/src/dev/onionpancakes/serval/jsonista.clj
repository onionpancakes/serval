(ns dev.onionpancakes.serval.jsonista
  (:require [dev.onionpancakes.serval.io.protocols :as p]
            [jsonista.core :as j])
  (:import [jakarta.servlet ServletResponse]))

(defn read-json
  ([ctx]
   (read-json ctx nil))
  ([ctx {:keys [to from error object-mapper]
         :or   {from          [:serval.service/request :reader]
                to            [:serval.request/body]
                error         [:serval.jsonista/error]
                object-mapper j/default-object-mapper}}]
   (try
     (let [source (get-in ctx from)
           value  (j/read-value source object-mapper)]
       (assoc-in ctx to value))
     (catch com.fasterxml.jackson.core.JsonParseException ex
       (assoc-in ctx error {:exception ex})))))

(defrecord JsonBody [value object-mapper]
  p/ResponseBody
  (p/write-body [this response]
    (j/write-value (.getWriter ^ServletResponse response) value object-mapper)))

(defn json-body
  ([value]
   (JsonBody. value j/default-object-mapper))
  ([value object-mapper]
   (JsonBody. value object-mapper)))
