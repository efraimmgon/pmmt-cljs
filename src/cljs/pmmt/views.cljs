(ns pmmt.views
  (:require
   [cljsjs.plotly]
   [cljsjs.jquery]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as re-frame :refer [subscribe]]
   [pmmt.pages.admin.core :as adm]
   ;; REVIEW: remove reg?
   [pmmt.components.registration :as reg]
   [pmmt.pages.base :refer [base-template]]
   [pmmt.pages.login :refer [login-template]]))

(defn modal []
  (let [modal-cmp (subscribe [:modal])]
    (fn []
      (when @modal-cmp
        [@modal-cmp]))))

(def pages
  {:home #'login-template
   :admin #'adm/main-template})

(defn page []
  (r/with-let [page (subscribe [:page])]
    [:div
     [modal]
     (if (= @page :admin)
       [(get pages @page)]
       [base-template (get pages @page)])]))
