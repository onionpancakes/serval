(ns dev.onionpancakes.serval.jsonista
  (:require [dev.onionpancakes.serval.io.body :as io.body]
            [jsonista.core :as j])
  (:import [jakarta.servlet ServletResponse]))

(def default-object-mapper
  j/default-object-mapper)

(def keyword-keys-object-mapper
  j/keyword-keys-object-mapper)

(defn read-json
  ([ctx]
   (read-json ctx nil))
  ([ctx {:keys [to from error object-mapper]
         :or   {from          [:serval.service/request :reader]
                to            [:serval.jsonista/value]
                error         [:serval.jsonista/error]
                object-mapper j/default-object-mapper}}]
   (try
     (let [source (get-in ctx from)
           value  (j/read-value source object-mapper)]
       (assoc-in ctx to value))
     (catch com.fasterxml.jackson.core.JsonParseException ex
       (assoc-in ctx error {:exception ex}))
     (catch com.fasterxml.jackson.databind.exc.MismatchedInputException ex
       (assoc-in ctx error {:exception ex})))))

(defrecord JsonBody [value options]
  io.body/ResponseBody
  (io.body/service-body [_ _ _ response]
    (let [object-mapper (:object-mapper options j/default-object-mapper)]
      (j/write-value (.getWriter ^ServletResponse response) value object-mapper))))

(defn json-body
  ([value]
   (JsonBody. value nil))
  ([value options]
   (JsonBody. value options)))
