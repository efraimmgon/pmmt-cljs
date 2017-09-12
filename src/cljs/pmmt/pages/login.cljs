(ns pmmt.pages.login
  (:require
   [reagent.core :as r :refer [atom]]
   [reagent-forms.core :refer [bind-fields]]
   [re-frame.core :as re-frame :refer [dispatch subscribe]]
   [pmmt.pages.base :refer [footer]]))


(def login-form-template
  [:div
   [:div.form-group
    [:label "Email address"]
    [:input.form-control
     {:field :text, :id :id, :placeholder "Enter username"}]]
   [:div.form-group
    [:label "Password"]
    [:input.form-control
     {:field :password, :id :pass, :placeholder "Enter password"}]]])

(defn login-form []
  (r/with-let [doc (atom {})
               errors (atom nil)]
    ;; if you want to have the card without animation please remove
    ;; the \".card-hidden\" class
    [:div.card
     [:div.header.text-center "Login"]
     [:div.content
      ;; TODO: username and password validation
      [bind-fields login-form-template doc]]
     [:div.footer.text-center
      [:button.btn.btn-fill.btn-warning.btn-wd
       {:on-click #(dispatch [:login doc errors])}
       "Login"]]]))

(defn login-template [color-palette background-image]
  (r/with-let [user (subscribe [:identity])]
    [:div.wrapper.wrapper-full-page
     [:div.full-page.login-page
      ;; you can change the color of the filter page using:
      ;; data-color=\"blue | azure | green | orange | red | purple\"
      {:data-image @background-image,
       :data-color @color-palette}
      [:div.content
       [:div.container
        [:div.row
         [:div.col-md-4.col-sm-6.col-md-offset-4.col-sm-offset-3
          (when-not @user
            [login-form])]]]]
      [footer]]]))
