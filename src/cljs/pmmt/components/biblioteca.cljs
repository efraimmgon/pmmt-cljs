(ns pmmt.components.biblioteca
  (:require [ajax.core :as ajax]
            [reagent.session :as session]))


(defn display-tags-and-docs [tags-docs]
  [:div
   (for [tag-docs tags-docs]
     ^{:key tag-docs}
    [:div
     [:h3 (:name (:tag tag-docs))]
     [:ul.list-group]
     (for [[idx doc] (map-indexed vector (:docs tag-docs))]
       ^{:key doc}
       [:li.list-group-item
        (inc idx) " - "
        [:a {:href (:url doc)}
         [:strong (:name doc)] " - "
         (:description doc)]])])])

(defn fetch-tags-documents! []
  (ajax/GET "/tags-and-documents"
            {:handler #(session/put! :tags-docs %)}))

(defn render-test [arg]
  (.log js/console arg)
  [:div
     [:h3 (:tag arg)]
     [:ul.list-group
      [:li
       (str (:docs arg))]]])


(defn biblioteca-page []
  (fetch-tags-documents!)
  [:div.container
   [:div.page-header
    [:h1 "Biblioteca " [:small "Links e documentos"]]]
   [:div.panel.panel-primary
    [:div.panel-heading
     [:h3.panel-title "Grupos"]]]
   [display-tags-and-docs (session/get :tags-docs)]])
