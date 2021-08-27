(ns dev.onionpancakes.serval.handler.http)

(defn response
  ([status body]
   (fn [ctx]
     (into ctx {:serval.response/status status
                :serval.response/body   body})))
  ([status body content-type]
   (fn [ctx]
     (into ctx {:serval.response/status       status
                :serval.response/body         body
                :serval.response/content-type content-type})))
  ([status body content-type character-encoding]
   (fn [ctx]
     (into ctx {:serval.response/status             status
                :serval.response/body               body
                :serval.response/content-type       content-type
                :serval.response/character-encoding character-encoding}))))

(defn set-headers
  [headers]
  (fn [ctx]
    (update ctx :serval.response/headers (fnil into {}) headers)))

