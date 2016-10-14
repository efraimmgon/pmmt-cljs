(ns pmmt.routes.services
  (:require [ring.util.http-response :refer :all]
            [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [pmmt.routes.home :as home]
            [pmmt.routes.geo :as geo]
            [pmmt.db.core :as db]))

(s/defschema Tag
             {:id s/Int
              :name String
              :description String})

(s/defschema Document
             {:id s/Int
              :name String
              :description String
              :url String})

(s/defschema TagDocuments
             {:tag Tag
              :docs [(assoc Document
                            :tag_id s/Int
                            :doc_id s/Int
                            :id_2 s/Int)]})

(s/defschema TimeDeltaParams
             {:date String
              :days String})

(s/defschema GeoRequest
             {:cidade Long
              :natureza Long
              :data_inicial String
              :data_final String
              (s/optional-key :bairro) String
              (s/optional-key :via) String
              (s/optional-key :hora_inicial) String
              (s/optional-key :hora_final) String})

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "PMMT Public API"
                           :description "Public Services"}}}}

  (GET "/tags-and-documents" req
       :summary "retrieve all tags and respective documents"
       :return [TagDocuments]
       (home/list-tags-documents))

  (GET "/calculate-delta" []
       :summary "Calculate a time delta"
       :query-params [date :- String, days :- Long]
       :return String
       (home/time-delta date days))
  (GET "/cidades" []
       :summary "List all `cidade` records"
       :return [{:id Long, :nome String}]
       (geo/get-cities))
  (GET "/naturezas" []
       :summary "List all `natureza` records"
       :return [{:id Long, :nome String}]
       (geo/get-naturezas))
  ;; Analise Criminal
  (GET "/analise-criminal/geo/dados" req
       :summary "Handle a user request and return a map response"
       :query-params [params GeoRequest]
       ; TODO: :return [GeoResponse]
       (geo/geo-dados params))
  ;; test route
  (GET "/sample-ocorrencias" []
       (ok (db/sample-ocorrencias))))
