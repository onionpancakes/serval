(ns dev.onionpancakes.serval.core-async
  (:require [clojure.core.async :as async :refer [<!!]]
            [dev.onionpancakes.serval.io.body :as io.body])
  (:import [jakarta.servlet ServletResponse]
           [java.util.concurrent CompletableFuture]))

(defrecord ChannelBody [ch]
  io.body/ResponseBody
  (io.body/service-body [_ _ _ response]
    (let [out (.getOutputStream ^ServletResponse response)
          rch (async/thread
                (loop [value (<!! ch)]
                  (when (some? value)
                    (io.body/write value out)
                    (recur (<!! ch)))))
          cf  (CompletableFuture.)
          _   (async/take! rch (fn [_] (.complete cf nil)))]
      cf)))

(defn channel-body
  [ch]
  (ChannelBody. ch))
