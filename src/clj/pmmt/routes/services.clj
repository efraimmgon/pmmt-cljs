(ns pmmt.routes.services
  (:require
   [ring.util.http-response :refer :all]
   [clojure.tools.logging :as log]
   [compojure.api.sweet :refer :all]
   [compojure.api.upload :refer
    [wrap-multipart-params TempFileUpload]]
   [schema.core :as s]
   [pmmt.routes.home :as home]
   [pmmt.routes.report :as report]
   [pmmt.routes.geo :as geo]
   [pmmt.db.core :as db]
   [pmmt.routes.admin :as admin]
   [pmmt.routes.services.auth :as auth]
   [pmmt.routes.services.upload :as upload]))

; ---------------------------------------------------------------------
; Misc
; ---------------------------------------------------------------------

(s/defschema Result
             {:result s/Keyword
              (s/optional-key :message) String})

(s/defschema TimeDeltaParams
             {:date String
              :days String})

; ---------------------------------------------------------------------
; Geo
; ---------------------------------------------------------------------

(s/defschema GeoRequest
             {; don't know how to coerce this here
              ; should be three options: number, coll, and string
              :crime_id s/Str
              :data_inicial s/Str
              :data_final s/Str
              (s/optional-key :bairro) s/Str
              (s/optional-key :via) s/Str
              (s/optional-key :hora_inicial) s/Str
              (s/optional-key :hora_final) s/Str})

; ---------------------------------------------------------------------
; Report
; ---------------------------------------------------------------------

(s/defschema ReportRequest
             {:data-inicial-a s/Str
              :data-final-a s/Str
              :data-inicial-b s/Str
              :data-final-b s/Str
              (s/optional-key :neighborhood) s/Str
              ; `Crimes`
              (s/optional-key :roubo) s/Bool
              (s/optional-key :furto) s/Bool
              (s/optional-key :trafico) s/Bool
              (s/optional-key :homicidio) s/Bool
              ; `Outros`
              (s/optional-key :weekday) s/Bool
              (s/optional-key :times) s/Bool})

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "PMMT Public API"
                           :description "Public Services"}}}}
  ; auth
  ; TODO: change to POST
  (GET "/register" req
        :return Result
        :query-params [id :- String, pass :- String, pass-confirm :- String]
        :summary "register a new user"
        (auth/register! req {:id id, :pass pass, :pass-confirm pass-confirm}))
  (POST "/login" req
        :header-params [authorization :- String]
        :summary "log in the user and create a session"
        :return Result
        (auth/login! req authorization))
  (POST "/logout" req
        :summary "remove user session"
        :return Result
        (auth/logout!))
  (GET "/send-message" req
        :query-params [a :- String]
        (ok (str "param: " a " // params: " (:params req))))


  (GET "/calculate-delta" []
       :summary "Calculate a time delta"
       :query-params [date :- String, days :- Long]
       :return String
       (home/time-delta date days))

  ;; Analise Criminal
  (GET "/analise-criminal/geo/dados" []
       :summary "Handle a user request and return a map response"
       :query [georeq GeoRequest]
       ; TODO: `return`
       (geo/geo-dados georeq))
  (GET "/analise-criminal/relatorio/dados" []
       :summary "Handle a user request and return a map response"
       :query [reportreq ReportRequest]
       ; TODO: `return`
       (report/report-data reportreq)))

(defapi restricted-service-routes
  {:swagger {:ui "/swagger-ui-private"
             :spec "/swagger-private.json"
             :data {:info {:version "1.0.0"
                           :title "PMMT API"
                           :description "Private Services"}}}}
  (GET "/db/users" []
       :summary "Retrieves all users"
       (admin/get-users))
  (GET "/db/:table" []
       :summary "Retrieves all rows in the given table"
       :path-params [table :- String]
       (admin/fetch-rows table))
  (GET "/db/:table/:field/:value" []
       :summary "Retrieves the rows matching the value in the given field and table"
       :path-params [table :- String
                     field :- String
                     value :- String]
       (admin/fetch-rows-by-value table field value))
  (GET "/db/crime-reports/geocode" []
       :summary "Retrive `crime-reports` rows with `latitude` = null"
       (admin/get-ungeocoded-reports))

  (POST "/upload" req
        :multipart-params [file :- TempFileUpload]
        :middleware [wrap-multipart-params]
        :summary "handles reports csv file upload"
        :return Result
        (upload/save-data! file))

  (context
   "/api" []
   :tags ["api"]
   (PUT "/crime-reports/update" req
        :summary "Update `crime-reports` with the given data, identified by `id`"
        :return Result
        (admin/update-crime-reports! (:body-params req)))))
