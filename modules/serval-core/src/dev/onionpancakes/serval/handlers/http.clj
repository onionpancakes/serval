(ns dev.onionpancakes.serval.handlers.http)

(defn response
  "Set the response status, body, content-type, and character encoding."
  ([ctx status]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status status))
  ([ctx status body]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status status
              :serval.response/body   body))
  ([ctx status body content-type]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status       status
              :serval.response/body         body
              :serval.response/content-type content-type))
  ([ctx status body content-type character-encoding]
   {:pre [(int? status)]}
   (assoc ctx :serval.response/status             status
              :serval.response/body               body
              :serval.response/content-type       content-type
              :serval.response/character-encoding character-encoding)))

(defn set-headers
  [ctx headers]
  (assoc ctx :serval.response/headers headers))
