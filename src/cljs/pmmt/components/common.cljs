(ns pmmt.components.common)

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

(defn input [type id placeholder fields]
  [:input.form-control
   {:type type
    :placeholder placeholder
    :value (id @fields)
    :on-change #(swap! fields assoc id (-> % .-target .-value))}])

(defn select-input [id options fields]
  [:select
   {:id id
    :class "form-control"
    :name id}
   (for [[value display] options]
     ^{:key value}
     [:option
      {:value value
       :on-change #(swap! fields assoc id (-> % .-target .-value))}
      display])])

(defn select-form [label id options fields]
  [:div.form-group
   [:label {:for id} label]
   [select-input id options fields]])

(defn form-input [type label id placeholder fields optional?]
  [:div.form-group
   [:label label]
   (if optional?
     [input type id placeholder fields]
     [:div.input-group
      [input type id placeholder fields]
      [:span.input-group-addon
       "*"]])])

(defn text-input [label id placeholder fields & [optional?]]
  (form-input :text label id placeholder fields optional?))

(defn password-input [label id placeholder fields & [optional?]]
  (form-input :password label id placeholder fields optional?))
