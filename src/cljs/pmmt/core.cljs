(ns pmmt.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [pmmt.components.utilitarios :as u]
            [pmmt.components.biblioteca :as b]
            [pmmt.components.geo :as geo])
  (:import goog.History))

(defn modal []
  (when-let [session-modal (session/get :modal)]
    [session-modal]))

(defn nav-link [uri title page]
  [:li
   {:class (when (= page (session/get :page)) "active")}
   [:a {:href uri} title]])

(defn navbar []
  (fn []
    [:nav
     {:role "navigation"
      :class "navbar navbar-default"}
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
      [:ul.nav.navbar-nav.pull-right
       [nav-link "#/login" "Login" :login]]]]))

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
      [:a {:href "#/"} "Análise Criminal"]]
     [:p.lead
      [:a {:href "#/utilitarios"} "Utilitários"]]
     [:h2 "Referências e documentos"]
     [:p.lead
      [:a {:href "#/"} "Biblioteca"]]]))


(def pages
  {:home #'home-page
   :utilitarios #'u/utilitarios-page
   :biblioteca #'b/biblioteca-page
   :geo #'geo/geo-page})

(defn page []
  [:div
   [modal]
   [(pages (session/get :page))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/utilitarios/" []
  (session/put! :page :utilitarios))

(secretary/defroute "/biblioteca/" []
  (session/put! :page :biblioteca))

(secretary/defroute "/analise-criminal/geo/" []
  (session/put! :page :geo))

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
  (session/put! :page :home)
  (mount-components))
