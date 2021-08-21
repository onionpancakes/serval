(ns dev.onionpancakes.serval.reitit
  (:require [reitit.core :as r]))

(defn match-handler
  [router]
  (fn [{:serval.request/keys [path method] :as ctx}]
    (let [match      (r/match-by-path router path)
          match-data (get-in match [:data method])]
      (cond-> ctx
        (contains? match-data :name)
        (assoc-in [:serval.reitit/match (:name match-data)] match)))))

(def ^:dynamic *match* nil)

(defn route-handler
  [router]
  (fn [{:serval.request/keys [path method] :as ctx}]
    (let [match       (r/match-by-path router path)
          method-data (get-in match [:data method])
          handler     (:handler method-data)]
      (binding [*match* match]
        (cond-> ctx
          (contains? method-data :name)
          (assoc-in [:serval.reitit/match (:name method-data)] match)
          
          true (handler))))))
