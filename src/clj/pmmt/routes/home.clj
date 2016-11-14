(ns pmmt.routes.home
  (:require [pmmt.layout :as layout]
            [pmmt.db.core :as db]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.java.io :as io]))

;;; Biblioteca

(defn tags-documents []
  (let [docs-tags (db/get-documents-and-tags-id)]
    (reduce (fn [acc tag]
              (conj acc {:tag tag
                         :docs (filter #(= (:id tag)
                                           (:tag_id %))
                                       docs-tags)}))
            [] (db/get-tags))))

(defn list-tags-documents []
  (response/ok
   (tags-documents)))

;;; Utilitários

(def date-format (tf/formatter "dd/MM/yyyy"))

(defn find-date [date days]
  (let [date (tf/parse date-format date)]
    (tf/unparse date-format
      (-> (t/plus date
                  (t/days days))
          ;; to return the final day
          (t/minus (t/days 1))))))

(defn time-delta [date days]
  (->> (find-date date days)
       (response/ok)))

;;; Views and routes

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/docs" [] (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                      (response/header "Content-Type" "text/plain; charset=utf-8"))))