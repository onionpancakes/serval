(ns dev.onionpancakes.serval.handler.http)

(defn response
  ([status body]
   (fn [ctx]
     (conj ctx {:serval.response/status status
                :serval.response/body   body})))
  ([status body content-type]
   (fn [ctx]
     (conj ctx {:serval.response/status       status
                :serval.response/body         body
                :serval.response/content-type content-type})))
  ([status body content-type character-encoding]
   (fn [ctx]
     (conj ctx {:serval.response/status             status
                :serval.response/body               body
                :serval.response/content-type       content-type
                :serval.response/character-encoding character-encoding}))))

(defn set-headers
  [headers]
  (fn [ctx]
    (update ctx :serval.response/headers merge headers)))

