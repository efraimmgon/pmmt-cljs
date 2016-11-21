(ns pmmt.core
  (:require [reagent.core :as r :refer [atom]]
            [reagent.session :as session]
            [ajax.core :as ajax :refer [GET POST]]
            [re-frame.core :as re-frame :refer
             [subscribe dispatch dispatch-sync]]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [pmmt.handlers]
            [pmmt.subs]
            [pmmt.components.registration :as reg]
            [pmmt.components.login :as login]
            [pmmt.components.utilitarios :as u]
            [pmmt.components.biblioteca :as b]
            [pmmt.components.geo :as geo]
            [pmmt.components.report :as report]
            [pmmt.components.admin :as admin]
            [pmmt.components.admin.core :as adm])
  (:import goog.History))

(enable-console-print!)

; Components -------------------------------------------------------------

(defn modal []
  (let [modal-cmp (subscribe [:modal])]
    (fn []
      (when @modal-cmp
        [@modal-cmp]))))

(defn nav-link [uri title page]
  (let [current-page (subscribe [:page])]
    (fn []
      [:li
       {:class (when (= page @current-page) "active")}
       [:a {:href uri} title]])))

(defn account-actions [user-id]
  (let [expanded? (r/atom false)]
    (fn []
      [:div.dropdown
       {:class (when @expanded? "open")
        :on-click #(swap! expanded? not)}
       [:button.btn.btn-secondary.dropdown-toggle
        {:type :button}
        [:span.glyphicon.glyphicon-user] " " user-id]
       [:div.dropdown-menu.user-actions
        [:a.dropdown-item.btn
         {:href "#/admin"}
         "admin"]
        [:a.dropdown-item.btn
         {:on-click #(dispatch [:modal reg/delete-account-modal])}
         "excluir conta"]
        [:a.dropdown-item.btn
         {:on-click #(dispatch [:logout])}
         "sair"]]])))

(defn user-menu []
  (let [user-id (subscribe [:identity])]
    (fn []
      (if @user-id
        [:ul.nav.navbar-nav.pull-right
         [:li [account-actions @user-id]]]
        [:ul.nav.navbar-nav.pull-right
         [:li [login/login-button]]
         [:li [reg/registration-button]]]))))

(defn navbar []
  (fn []
    [:nav#navbar.navbar.navbar-default
     {:role "navigation"}
     [:div.navbar-header
      [:button
       {:data-target "#navbarCollapse"
        :data-toggle "collapse"
        :class "navbar-toggle"}
       [:span.sr-only "Toggle navigation"]
       [:span.icon-bar]
       [:span.icon-bar]
       [:span.icon-bar]]
      [:a.navbar-brand {:href "http://www.pm.mt.gov.br"} "PMMT"]]
     [:div#navbarCollapse.collapse.navbar-collapse
      [:ul.nav.navbar-nav
       [nav-link "#/" "Home" :home]
       [nav-link "#/analise-criminal/geo/" "Georreferenciamento" :geo]
       [nav-link "#/analise-criminal/relatorio/" "Relatório" :relatorio]
       [nav-link "#/biblioteca/" "Biblioteca" :biblioteca]
       [nav-link "#/utilitarios/" "Utilitários" :utilitarios]
       [nav-link "#/admin-old/" "Admin-old" :admin-old]]
      [user-menu]]]))

(defn home-page []
  (fn []
    [:div.container
     [:div.page-header
      [:h1 "Índice " [:small "Geral"]]]
     [:div.alert.alert-dismissible.alert-warning
      [:h4 "Beta"]
      [:p "This is still a work in progress."]]
     [:h2 "Aplicativos"]
     [:p.lead
      [:a {:href "#/analise-criminal"} "Análise Criminal"]]
     [:p.lead
      [:a {:href "#/utilitarios"} "Utilitários"]]
     [:h2 "Referências e documentos"]
     [:p.lead
      [:a {:href "#/"} "Biblioteca"]]]))

(def pages
  {:home #'home-page
   :admin #'adm/admin-page
   :admin-old #'admin/admin-page
   :utilitarios #'u/utilitarios-page
   :biblioteca #'b/biblioteca-page
   :relatorio #'report/report-page
   :geo #'geo/geo-page})

(defn page []
  (let [page (subscribe [:page])
        reload? (atom false)]
    (fn []
      ; hack to clear admin and other pages breaks
      (when (= @page :admin)
        (reset! reload? true))
      (when (and @reload? (= @page :home))
        (.reload js/location))
      [:div
       [modal]
       [(pages @page)]])))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (dispatch [:page :home]))

(secretary/defroute "/admin" []
  (dispatch [:page :admin]))

(secretary/defroute "/admin-old/" []
  (dispatch [:page :admin-old]))

(secretary/defroute "/utilitarios/" []
  (dispatch [:page :utilitarios]))

(secretary/defroute "/biblioteca/" []
  (dispatch [:page :biblioteca]))

(secretary/defroute "/analise-criminal/relatorio/" []
  (dispatch [:page :relatorio]))

(secretary/defroute "/analise-criminal/geo/" []
  (dispatch [:page :geo]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))


(defn init! []
  (hook-browser-navigation!)
  (dispatch-sync [:set-initial-state])
  (dispatch-sync [:page :home])
  (dispatch-sync [:set-identity js/identity])
  (mount-components))
