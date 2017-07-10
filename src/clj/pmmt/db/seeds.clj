(ns pmmt.db.seeds
  (:require [buddy.hashers :as hashers]
            [pmmt.db.core :as db]
            [pmmt.routes.services.upload :as upload]))

; (defn read-csv [f]
;   (upload/csv->map (upload/read-csv-file f)))
;
; ; reset db
; (defn reset-db! []
;   (db/delete-all-natureza!)
;   (db/delete-all-ocorrencia!)
;   (db/delete-all-users!))
;
; ; data
; (def users
;   [{:id "admin", :pass (hashers/encrypt "admin")}])
;
; (def naturezas
;   (read-csv "/Users/efraimmgon/relatorio-db/db_naturezas.csv"))
;
; (def ocorrencias
;   (read-csv "/Users/efraimmgon/relatorio-db/db_ocorrencias.csv"))
;
; (defn insert-data! []
;   ; create users
;   (doseq [user users]
;     (db/create-user! user))
;
;   ; create naturezas
;   (doseq [natureza naturezas]
;     (db/create-natureza-with-id! (upload/natureza-coercer natureza)))
;
;   ; create ocorrencias
;   (doseq [ocorrencia ocorrencias]
;     (db/create-ocorrencia! (upload/reports-coercer ocorrencia))))
