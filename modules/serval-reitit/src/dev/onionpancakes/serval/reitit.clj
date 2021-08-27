(ns dev.onionpancakes.serval.reitit
  (:require [reitit.core :as r]))

(defn match-by-path
  ([ctx router]
   (match-by-path ctx router nil))
  ([ctx router {:keys [path-key match-key]
                :or   {path-key  [:serval.service/request :path]
                       match-key [:serval.reitit/match]}}]
   (let [path  (get-in ctx path-key)
         match (r/match-by-path router path)]
     (assoc-in ctx match-key match))))

(defn handle-match-by-method
  ([ctx]
   (handle-match-by-method ctx nil))
  ([ctx {:keys [match-key default]
         :or   {match-key [:serval.reitit/match]
                default   identity}}]
   (let [match   (get-in ctx match-key)
         method  (get-in ctx [:serval.service/request :method])
         handler (get-in match [:data method :handler] default)]
     (handler ctx))))
