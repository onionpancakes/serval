(ns dev.onionpancakes.serval.reitit
  (:require [reitit.core :as r]))

;; Match handler priority
;; 1. Handler found under capitalized method key.
;; e.g. {:GET {:handler (fn [ctx] ...)}}
;; 2. Handler under :handler key
;; e.g. {:handler (fn [ctx] ...)}
;; 3. Handler given in default arg.

(defn get-match-handler
  "Return the handler mapped to method or default is not found."
  [match method default]
  (let [data (:data match)]
    (or (get-in data [method :handler])
        (get data :handler default))))

(defn get-route-path
  "Return path-info as path, or root path \"/\" if path-info is absent."
  [ctx]
  (get-in ctx [:serval.service/request :path-info] "/"))

(defn route
  ([ctx router]
   (route ctx router nil))
  ([ctx router {:keys [path-fn match-key default]
                :or   {path-fn   get-route-path
                       match-key :serval.reitit/match
                       default   identity}}]
   (let [path    (path-fn ctx)
         match   (r/match-by-path router path)
         method  (get-in ctx [:serval.service/request :method])
         handler (get-match-handler match method default)]
     (-> (assoc ctx match-key match)
         (handler)))))

(def router
  r/router)
