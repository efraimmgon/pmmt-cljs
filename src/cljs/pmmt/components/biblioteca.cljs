(ns pmmt.components.biblioteca
  (:require [ajax.core :as ajax]
            [re-frame.core :refer
             [reg-event-db reg-event-fx reg-sub subscribe dispatch dispatch-sync]]
            [day8.re-frame.http-fx]))

; Events ---------------------------------------------------------------

(reg-event-db
 :tags-docs
 (fn [db [_ tags-docs]]
   (assoc db :tags-docs tags-docs)))

(reg-event-fx
 :query-tags-documents
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri "/tags-and-documents"
                 :on-success [:tags-docs]
                 :response-format (ajax/json-response-format {:keywords? true})}
    :db db}))

; Subscriptions ---------------------------------------------------------

(reg-sub
 :tags-docs
 (fn [db _]
   (:tags-docs db)))

; Components ------------------------------------------------------------

(defn display-tags-and-docs []
  (let [tags-docs (subscribe [:tags-docs])]
    (fn []
      [:div
       (for [tag-docs @tags-docs]
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
             (:description doc)]])])])))

(defn biblioteca-page []
  (dispatch-sync [:query-tags-documents])
  (fn []
    [:div.container
     [:div.page-header
      [:h1
       "Biblioteca " [:small "Links e documentos"]]]
     [:div.panel.panel-primary
      [:div.panel-heading
       [:h3 "Grupos"]]]
     [display-tags-and-docs]]))
