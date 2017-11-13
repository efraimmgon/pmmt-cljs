(ns pmmt.charts)

(def colors
  ["#001f3f" ; navy
   "#0074D9" ; blue
   "#7FDBFF" ; aqua
   "#39CCCC" ; teal
   "#3D9970" ; olive
   "#2ECC40" ; green
   "#01FF70" ; lime
   "#FFDC00" ; yellow
   "#FF851B" ; orange
   "#FF4136" ; red
   "#85144b" ; maroon
   "#F012BE" ; fuchsia
   "#B10DC9" ; purple
   "#111111" ; black
   "#AAAAAA" ; gray
   "#DDDDDD"]) ;silver

(defmulti chart-opts :type)

; dataset is a vector of ints
(defmethod chart-opts :pie
  [{:keys [id labels datasets]}]
  {:type :pie
   :data {:labels labels
          :datasets [{:data datasets
                      :backgroundColor (take (count labels) colors)}]}})

; dataset is a vector maps with the keys: `data` and `label`
(defmethod chart-opts :line
  [{:keys [id labels datasets]}]
  {:type :line
   :data {:labels labels
          :datasets (map-indexed
                     (fn [i row]
                       {:data (:data row)
                        :label (:label row)
                        :borderColor (colors i)
                        :fill false})
                     datasets)}})

(defmethod chart-opts :bar
  [{:keys [id labels datasets]}]
  {:type :bar
   :data {:labels labels
          :datasets [{:data datasets
                      :backgroundColor (take (count labels) colors)}]}
   :options {:legend {:display false}}})

(defmethod chart-opts :grouped-bar
  [{:keys [id labels datasets]}]
  {:type :bar
   :data {:labels labels
          :datasets (map-indexed
                     (fn [i row]
                       {:data (:data row)
                        :label (:label row)
                        :backgroundColor (colors i)
                        :fill false})
                     datasets)}
   :options {:legend {:display false}}})

(defmethod chart-opts :horizontal-bar
  [{:keys [id labels datasets]}]
  {:type :horizontalBar
   :data {:labels labels
          :datasets [{:data datasets
                      :backgroundColor (take (count labels) colors)}]}
   :options {:legend {:display false}}})

(defmethod chart-opts :grouped-horizontal-bar
  [{:keys [id labels datasets]}]
  {:type :horizontalBar
   :data {:labels labels
          :datasets (map-indexed
                     (fn [i row]
                       {:data (:data row)
                        :label (:label row)
                        :backgroundColor (colors i)
                        :fill false})
                     datasets)}
   :options {:legend {:display false}}})

(defmethod chart-opts :radar
  [{:keys [id labels datasets]}]
  {:type :radar
   :data {:labels labels
          :datasets (map-indexed
                     (fn [i row]
                       {:data (:data row)
                        :label (:label row)
                        :backgroundColor (colors i)
                        :borderColor (colors i)
                        :pointBorderColor "#fff"
                        :pointBackgroundColor (colors i)
                        :fill true})
                     datasets)}})
