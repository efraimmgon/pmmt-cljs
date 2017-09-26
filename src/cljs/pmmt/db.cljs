(ns pmmt.db)

(def default-db
  {:identity (js->clj js/identity :keywordize-keys true)
   :page :home
   :sinop {:lat -11.8608456, :lng -55.50954509999997}
   :admin {:active-panel :dashboard
           :database {:setup-ready? false
                      :active-panel :database
                      ;; TODO: move to config file
                      :tables ["cities", "crimes", "crime_reports", "modes_desc"]}}
   :settings {:page-background-image "/img/full-screen-image-1.jpg"
              :page-color-palette "black"
              :sidebar-background-image "/img/sidebar-5.jpg"
              :sidebar-color-palette "black"
              :google-api-key "AIzaSyA7ekvGGHxVTwTVcpi073GOERnktqvmYz8"}})
