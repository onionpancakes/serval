(ns dev.onionpancakes.serval.handlers.http)

(defn response
  "Set the response status, body, content-type, and character encoding."
  ([ctx status]
   {:pre [(int? status)]}
   (-> (transient ctx)
       (assoc! :serval.response/status status)
       (persistent!)))
  ([ctx status body]
   {:pre [(int? status)]}
   (-> (transient ctx)
       (assoc! :serval.response/status status
               :serval.response/body   body)
       (persistent!)))
  ([ctx status body content-type]
   {:pre [(int? status)]}
   (-> (transient ctx)
       (assoc! :serval.response/status       status
               :serval.response/body         body
               :serval.response/content-type content-type)
       (persistent!)))
  ([ctx status body content-type character-encoding]
   {:pre [(int? status)]}
   (-> (transient ctx)
       (assoc! :serval.response/status             status
               :serval.response/body               body
               :serval.response/content-type       content-type
               :serval.response/character-encoding character-encoding)
       (persistent!))))

(defn set-headers
  "Set context response headers given key-value pairs, replacing previous
  header values at given keys and preserving header values at keys not specified.
  Keyword keys will be converted to strings."
  [ctx & kvs]
  (->> (partition 2 kvs)
       (eduction (map (juxt (comp name first) second)))
       (reduce (fn [ret [k v]]
                 (update ret k (fnil conj []) v))
               {})
       (update ctx :serval.response/headers (fnil into {}))))

