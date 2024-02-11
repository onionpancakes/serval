(ns dev.onionpancakes.serval.examples.todo.app
  (:require [dev.onionpancakes.serval.core :as srv]
            [dev.onionpancakes.serval.chassis :as srv.html]
            [dev.onionpancakes.chassis.core :as html]))

;; Page

(defn current-year []
  (java.time.Year/now))

(defmethod html/resolve-alias ::Layout
  [_ _ attrs content]
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

(defn not-found-page
  [_]
  [::Layout
   [:p "Page not found."]
   [:p [:a {:href "/"} "Return to Home"]]])

(defn error-page
  [_]
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
        [:span {:style {:text-decoration (if (:done? task)
                                           :line-through)}}
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
     " "
     [:button "Add task"]]
    (html/raw "&nbsp;")
    [:form {:action "/submit" :method :POST}
     [:input {:name :action :hidden true :value "clear"}]
     (let [some-done? (true? (some :done? (:tasks data)))]
       [:button {:disabled (not some-done?)} "Clear tasks"])]]])

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
  [ctx]
  (let [body (srv.html/html-writable (app-page @data))]
    (srv/response ctx 200 body "text/html" "UTF-8")))

(defn submit
  [{:serval.context/keys [request response]}]
  (let [_ (do-submit-action (:parameters request))]
    (.sendRedirect response "/")))

(defn not-found
  [ctx]
  (let [body (srv.html/html-writable (not-found-page nil))]
    (srv/response ctx 404 body "text/html" "UTF-8")))

(defn error
  [ctx]
  (let [body (srv.html/html-writable (error-page nil))]
    (srv/response ctx 404 body "text/html" "UTF-8")))

(def app
  {:routes      [["" #{:GET} index]
                 ["/submit" #{:POST} submit]                 
                 ["/not-found" #{:GET} not-found]
                 ["/error" #{:GET} error]]
   :error-pages {404       "/not-found"
                 405       "/not-found"
                 Throwable "/error"}})

(def app-dev
  {:routes      [["" #{:GET} #'index]
                 ["/submit" #{:POST} #'submit]
                 ["/not-found" #{:GET} #'not-found]
                 ["/error" #{:GET} error]]
   :error-pages {404       "/not-found"
                 405       "/not-found"}})
