(ns dev.onionpancakes.serval.jsonista
  (:require [dev.onionpancakes.serval.io :as io]
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

(defrecord Json [value options]
  io/ResponseBody
  (io/async-body? [this] false)
  (io/write-body [this response]
    (let [object-mapper (get options :object-mapper j/default-object-mapper)]
      (j/write-value (.getWriter ^ServletResponse response) value object-mapper))))

(defn json
  ([value]
   (Json. value nil))
  ([value options]
   (Json. value options)))
