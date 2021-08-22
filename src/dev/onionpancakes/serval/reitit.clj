(ns dev.onionpancakes.serval.reitit
  (:require [reitit.core :as r]))

(defn match-handler
  [router]
  (fn [{:serval.request/keys [path] :as ctx}]
    (let [match (r/match-by-path router path)]
      (assoc ctx :serval.reitit/match match))))

(defn not-found
  [ctx]
  (conj ctx {:serval.response/status 404
             :serval.response/body   "Not found!"}))

(defn invoke-match-handler
  [{:serval.request/keys [method] :as ctx} opts]
  (let [default (:not-found opts not-found)
        match   (get ctx :serval.reitit/match)
        handler (get-in match [:data method :handler] default)]
    (handler ctx)))

(defn route-handler
  ([router] (route-handler router nil))
  ([router opts]
   (comp #(invoke-match-handler % opts) (match-handler router))))
