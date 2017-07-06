(ns pmmt.views
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [pmmt.components.admin :as admin]
   [pmmt.components.admin.core :as adm]
   [pmmt.components.biblioteca :as b]
   [pmmt.pages.geoprocessing :as geo]
   [pmmt.components.login :as login]
   [pmmt.components.registration :as reg]
   [pmmt.pages.report :as report]
   [pmmt.pages.utils :as u]))


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
       [nav-link "#/utilitarios/" "Utilitários" :utilitarios]]
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
   :utilitarios #'u/main-page
   :biblioteca #'b/biblioteca-page
   :relatorio #'report/main-page
   :geo #'geo/main-page})

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
