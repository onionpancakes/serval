(ns dev.onionpancakes.serval.jsonista
  (:require [dev.onionpancakes.serval.io.body :as io.body]
            [jsonista.core :as j])
  (:import [jakarta.servlet ServletResponse]
           [java.nio ByteBuffer]))

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
       (assoc-in ctx error {:exception ex}))
     (catch com.fasterxml.jackson.databind.exc.MismatchedInputException ex
       (assoc-in ctx error {:exception ex})))))

(defrecord JsonBody [value options]
  io.body/ResponseBody
  (io.body/async-body? [this _] false)
  (io.body/write-body [this {^ServletResponse resp :serval.service/response}]
    (let [object-mapper (:object-mapper options j/default-object-mapper)]
      (j/write-value (.getWriter resp) value object-mapper)))

  io.body/AsyncWritable
  (io.body/write-listener [this ctx cf]
    (let [object-mapper         (:object-mapper options j/default-object-mapper)
          ^ServletResponse resp (:serval.service/response ctx)]
      (-> (j/write-value-as-bytes value object-mapper)
          (ByteBuffer/wrap)
          (io.body/buffer-write-listener (.getOutputStream resp) cf)))))

(defn json-body
  ([value]
   (JsonBody. value nil))
  ([value options]
   (JsonBody. value options)))
