(ns dev.onionpancakes.serval.core-async
  (:require [clojure.core.async :as async :refer [<!!]]
            [dev.onionpancakes.serval.service.body
             :as service.body])
  (:import [jakarta.servlet ServletResponse]
           [java.util.concurrent CompletableFuture]))

(defrecord ChannelBody [ch]
  service.body/Body
  (async-body? [_] true)
  (service-body [_ _ _ response]
    (let [out (.getOutputStream ^ServletResponse response)
          enc (.getCharacterEncoding ^ServletResponse response)
          rch (async/thread
                (loop [value (<!! ch)]
                  (when (some? value)
                    (service.body/write value out enc)
                    (recur (<!! ch)))))
          cf  (CompletableFuture.)
          _   (async/take! rch (fn [_] (.complete cf nil)))]
      cf)))

(defn channel-body
  [ch]
  (ChannelBody. ch))
