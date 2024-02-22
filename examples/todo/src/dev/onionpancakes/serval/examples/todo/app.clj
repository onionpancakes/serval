(ns dev.onionpancakes.serval.examples.todo.app
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.chassis :as srv.html]
            [dev.onionpancakes.chassis.core :as html]))

;; Page

(defn current-year []
  (.getValue (java.time.Year/now)))

(defmethod html/resolve-alias ::Layout
  [_ attrs content]
  [html/doctype-html5
   [:html {:lang "en"}
    [:head
     [:title "My Todo List"]]
    [:body
     [:header
      [:h1 [:a {:href  "/"
                :style {:color :black}}
            "My Todo List"]]]
     [:main attrs content]
     [:footer
      [:hr {:style {:opacity 0.3}}]
      [:p {:style {:font-size :8pt
                   :color     :grey}}
       "My Todo List Inc " current-year]]]]])

(def not-found-page
  [::Layout
   [:p "Page not found."]
   [:p [:a {:href "/"} "Return to Home"]]])

(def error-page
  [::Layout
   [:p "An error occured."]
   [:p [:a {:href "/"} "Return to Home"]]])

(defn app-page
  [data]
  [::Layout
   [:ol
    (for [[idx task] (map-indexed vector (:tasks data))]
      [:li {:style {:margin :4px}}
       [:form {:action "/submit" :method :POST}
        [:span (if (:done? task)
                 {:style {:text-decoration :line-through}})
         (:task task)]
        " - "
        [:span
         [:input {:name :action :hidden true :value "done"}]
         [:input {:name :task-idx :hidden true :value idx}]
         [:button {:disabled (:done? task)} "Done"]]]])]
   [:div {:style {:display :flex}}
    [:form {:action "/submit" :method :POST}
     [:input {:name :action :hidden true :value "add-task"}]
     [:input {:name :task :autofocus true :placeholder "Enter todo task"}]
     html/nbsp
     [:button "Add task"]]
    html/nbsp
    [:form {:action "/submit" :method :POST}
     [:input {:name :action :hidden true :value "clear"}]
     (let [some-done? (true? (some :done? (:tasks data)))]
       [:button {:disabled (not some-done?)} "Clear done"])]]])

;; Database

(defonce data
  (atom {}))

(defmulti do-submit-action
  (fn [params] (first (get params "action"))))

(defmethod do-submit-action "add-task"
  [params]
  (let [task (first (get params "task"))]
    (swap! data update :tasks (fnil conj []) {:task task})))

(defmethod do-submit-action "done"
  [params]
  (let [idx (parse-long (first (get params "task-idx")))]
    (swap! data update-in [:tasks idx] assoc :done? true)))

(defmethod do-submit-action "clear"
  [params]
  (let [clear-fn (fn [tasks]
                   (into [] (remove :done?) tasks))]
    (swap! data update :tasks clear-fn)))

;; Handlers

(defn index
  [_ _ response]
  (let [body (srv.html/html-writable (app-page @data))]
    (doto response
      (srv/set-http :content-type "text/html"
                    :character-encoding "utf-8")
      (srv/write-body body))))

(defn submit
  [_ request response]
  (let [_ (do-submit-action (:parameters request))]
    (srv/send-redirect response "/")))

(defn not-found
  [_ _ response]
  (let [body (srv.html/html-writable not-found-page)]
    (doto response
      (srv/set-http :status 404
                    :content-type "text/html"
                    :character-encoding "utf-8")
      (srv/write-body body))))

(defn error
  [_ _ response]
  (let [body (srv.html/html-writable error-page)]
    (doto response
      (srv/set-http :status 500
                    :content-type "text/html"
                    :character-encoding "utf-8")
      (srv/write-body body))))

(def app
  {:routes      [["" #{:GET} index]
                 ["/submit" #{:POST} submit]
                 ["/not-found" not-found]
                 ["/error" error]]
   :error-pages {404       "/not-found"
                 405       "/not-found"
                 Throwable "/error"}})

(def app-dev
  {:routes      [["" #{:GET} #'index]
                 ["/submit" #{:POST} #'submit]
                 ["/not-found" #'not-found]
                 ["/error" #'error]]
   :error-pages {404 "/not-found"
                 405 "/not-found"}})
