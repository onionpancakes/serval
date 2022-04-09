(ns dev.onionpancakes.serval.test-utils.client
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.serval.test-utils.server :as server
             ])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpResponse
            HttpRequest$Builder HttpRequest$BodyPublishers
            HttpResponse$BodyHandlers]))

(def default-uri-string
  (str "http://localhost:" server/port))

(defprotocol BodyPublisher
  (to-body-publisher [this]))

(extend-protocol BodyPublisher
  (Class/forName "[B")
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofByteArray this))
  String
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofString this))
  nil
  (to-body-publisher [_]
    (HttpRequest$BodyPublishers/noBody)))

(defn ^HttpRequest$Builder set-request-builder-headers
  [^HttpRequest$Builder builder headers]
  (doseq [[hname values] headers
          value          values]
    (.setHeader builder hname value))
  builder)

(defn build-request
  [req]
  (-> (HttpRequest/newBuilder)
      (.uri (URI. (:uri req default-uri-string)))
      (.method (:method req "GET") (to-body-publisher (:body req)))
      (set-request-builder-headers (:headers req))
      (.build)))

(defn build-response-map
  [^HttpResponse resp]
  {:status  (.statusCode resp)
   :headers (->> (.headers resp)
                 (.map)
                 (into {} (map (juxt key (comp vec val)))))
   :body    (.body resp)})

;; Send

(defonce ^HttpClient client
  (.. (HttpClient/newBuilder)
      (build)))

(defn send
  [req]
  (let [bh (HttpResponse$BodyHandlers/ofString)]
    (-> (.send client (build-request req) bh)
        (build-response-map))))
