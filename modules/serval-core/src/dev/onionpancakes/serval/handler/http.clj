(ns dev.onionpancakes.serval.handler.http)

(defn response
  "Set the response status, body, content-type, and character encoding."
  ([ctx status body]
   {:pre [(int? status)]}
   (into (or ctx {}) {:serval.response/status status
                      :serval.response/body   body}))
  ([ctx status body content-type]
   {:pre [(int? status)]}
   (into (or ctx {}) {:serval.response/status       status
                      :serval.response/body         body
                      :serval.response/content-type content-type}))
  ([ctx status body content-type character-encoding]
   {:pre [(int? status)]}
   (into (or ctx {}) {:serval.response/status             status
                      :serval.response/body               body
                      :serval.response/content-type       content-type
                      :serval.response/character-encoding character-encoding})))

(defn set-headers
  [ctx headers]
  (update ctx :serval.response/headers (fnil into {}) headers))

