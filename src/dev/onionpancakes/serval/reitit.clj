(ns dev.onionpancakes.serval.reitit
  (:require [reitit.core :as r]))

(defn match-handler
  [router]
  (fn [{:serval.request/keys [path method] :as ctx}]
    (let [match      (r/match-by-path router path)
          match-key  (-> (get-in match [:data method])
                         (find :key))]
      (cond-> ctx
        match-key (assoc-in [:serval.reitit/match (val match-key)] match)))))

(def ^:dynamic *match* nil)

(defn route-handler
  [router]
  (fn [{:serval.request/keys [path method] :as ctx}]
    (let [match       (r/match-by-path router path)
          method-data (get-in match [:data method])
          match-key   (find method-data :key)
          handler     (:handler method-data)]
      (binding [*match* match]
        (cond-> ctx
          match-key (assoc-in [:serval.reitit/match (val match-key)] match)
          true      (handler))))))
