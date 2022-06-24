(ns dev.onionpancakes.serval.handler.http)

(defn response
  ([ctx status body]
   {:pre [(int? status)]}
   (into ctx {:serval.response/status status
              :serval.response/body   body}))
  ([ctx status body content-type]
   {:pre [(int? status)]}
   (into ctx {:serval.response/status       status
              :serval.response/body         body
              :serval.response/content-type content-type}))
  ([ctx status body content-type character-encoding]
   {:pre [(int? status)]}
   (into ctx {:serval.response/status             status
              :serval.response/body               body
              :serval.response/content-type       content-type
              :serval.response/character-encoding character-encoding})))

#_(defn set-headers
  [ctx headers]
  (update ctx :serval.response/headers (fnil into {}) headers))

