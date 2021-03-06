(ns dev.onionpancakes.serval.reitit
  (:require [reitit.core :as r]))

(def router
  r/router)

(defn match-by-path
  ([ctx router]
   (match-by-path ctx router nil))
  ([ctx router {:keys [path-in match-in]
                :or   {path-in  [:serval.service/request :path]
                       match-in [:serval.reitit/match]}}]
   (let [path  (get-in ctx path-in)
         match (r/match-by-path router path)]
     (assoc-in ctx match-in match))))

(defn handle-match-by-method
  ([ctx]
   (handle-match-by-method ctx nil))
  ([ctx {:keys [match-in default]
         :or   {match-in [:serval.reitit/match]
                default  identity}}]
   (let [match   (get-in ctx match-in)
         method  (get-in ctx [:serval.service/request :method])
         handler (get-in match [:data method :handler])]
     (if (some? handler)
       (handler ctx)
       (default ctx)))))

(defn route
  ([ctx router]
   (route ctx router nil))
  ([ctx router opts]
   (-> (match-by-path ctx router opts)
       (handle-match-by-method opts))))
