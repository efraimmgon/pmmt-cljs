(ns pmmt.components.common
  (:require [dommy.core :as dommy :refer-macros [sel sel1]]))

; --------------------------------------------------------------------
; MISC
; --------------------------------------------------------------------

(defn modal [header body footer]
  [:div
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header [:h3 header]]
     [:div.modal-body body]
     [:div.modal-footer
      [:div.bootstrap-dialog-footer
       footer]]]]
   [:div.modal-backdrop.fade.in]])

(defn display-error [errors id]
  (when-let [error (id @errors)]
    [:div.alert.alert-danger (clojure.string/join error)]))

; --------------------------------------------------------------------
; BUTTON
; --------------------------------------------------------------------

(defn nav-button [{:keys [handler title]}]
  [:span
   [:button.btn.btn-default
    {:on-click handler}
    title]
   " "])

; --------------------------------------------------------------------
; FORMS
; --------------------------------------------------------------------

; Component parts

(defn select-input [id options fields]
  ;; set default value
  (swap! fields assoc id (-> options first :value))
  [:select.form-control
   {:id id
    :style {:height "43px"}
    :name id
    :on-change #(swap! fields assoc id (-> % .-target .-value))}
   (for [{:keys [value display]} options]
     ^{:key value}
     [:option
      {:value (str value)}
      display])])

(defn input [type id placeholder fields]
  [:input.form-control
   {:type type
    :placeholder placeholder
    :value (id @fields)
    :on-change #(swap! fields assoc id (-> % .-target .-value))}])

(defn form-input [type label id placeholder fields optional?]
  [:div.form-group
   [:label label]
   (if optional?
     [input type id placeholder fields]
     [:div.input-group
      [input type id placeholder fields]
      [:span.input-group-addon
       "*"]])])

(defn checkbox-input-group [label id value fields]
  (let [checked? (atom false)]
    [:label
      [:input
       {:type :checkbox,
        :value value,
        :on-change #(do (swap! checked? not)
                        (if @checked?
                          (swap! fields assoc id (-> % .-target .-value))
                          (swap! fields dissoc id)))}]
     label]))

; Components

;; Text

(defn text-input [label id placeholder fields & [optional?]]
  (form-input :text label id placeholder fields optional?))

;; Number

(defn number-input [label id placeholder fields & [optional?]]
  (form-input :number label id placeholder fields optional?))

;; Password

(defn password-input [label id placeholder fields & [optional?]]
  (form-input :password label id placeholder fields optional?))

;; Checkbox

(defn checkbox-input [label id value fields & [optional?]]
  [:div.checkbox
   (if optional?
     [checkbox-input-group label id value fields]
     [:div.input-group
      checkbox-input-group label id value fields
      [:span.input-group-addon
       "*"]])])

;; Select

(defn select-form [label id options fields & [optional?]]
  [:div.form-group
   [:label {:for id} label]
   (if optional?
     [select-input id options fields]
     [:div.input-group
      [select-input id options fields]
      [:span.input-group-addon
       "*"]])])

; --------------------------------------------------------------------
; PAGER
; --------------------------------------------------------------------

(defn forward [i pages]
  (if (< i (dec pages)) (inc i) i))

(defn back [i]
  (if (pos? i) (dec i) i))

(defn nav-link [page i]
  [:li.page-item>a.page-link.btn.btn-primary
   {:on-click #(reset! page i)
    :class (when (= i @page) "active")}
   [:span i]])

(defn pager [pages page]
  (when (> pages 1)
    (into
     [:div.text-xs-center>ul.pagination.pagination-lg]
     (concat
      [[:li.page-item>a.page-link.btn
        {:on-click #(swap! page back pages)
         :class (when (= @page 0) "disabled")}
        [:span "<<"]]]
      (map (partial nav-link page) (range pages))
      [[:li.page-item>a.page-link.btn
        {:on-click #(swap! page forward pages)
         :class (when (= @page (dec pages)) "disabled")}
        [:span ">>"]]]))))

; table ------------------------------------------------------------

(defn thead [headers]
  [:thead
   [:tr
    (for [h headers]
      ^{:key h}
      [:th h])]])

(defn tbody [rows]
  [:tbody
   (for [row rows]
     ^{:key row}
     [:tr
      (for [k (keys row)]
        ^{:key k}
        [:td (k row)])])])

; --------------------------------------------------------------
; DOM Manipulation
; --------------------------------------------------------------

(defn set-attrs! [elt opts]
  (reduce (fn [elt- [attr val]]
            (dommy/set-attr! elt- attr val))
          elt opts))

(defn add-style! [opts]
  (let [elt (-> (dommy/create-element :link)
                (dommy/set-attr! :rel "stylesheet")
                (dommy/set-attr! :type "text/css")
                (dommy/set-attr! :href (:href opts)))]
    (dommy/append! (sel1 :body) elt)))

(defn add-script! [opts]
  (let [elt (-> (dommy/create-element :script)
                (dommy/set-attr! :type "text/javascript")
                (dommy/set-attr! :src (:src opts)))]
    (dommy/append! (sel1 :body) elt)))

; remove a style
(defn remove-elt! [id]
  (dommy/remove! (sel1 id)))
