(ns pmmt.handlers.geocode
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [ajax.core :as ajax]
   [re-frame.core :refer [dispatch dispatch-sync reg-event-db reg-sub subscribe]]
   [pmmt.subs :refer [query]]))


; ----------------------------------------------------------------------
; Subs
; ----------------------------------------------------------------------

(reg-sub
 :geocode/ready?
 (fn [db _]
   (get-in db [:geocode :ready?])))

(reg-sub
 :geocode/result
 (fn [db _]
   (get-in db [:geocode :result])))

(reg-sub
 :geocode/zero-results
 (fn [db _]
   (get-in db [:geocode :zero-results])))


; ----------------------------------------------------------------------
; Helpers
; ----------------------------------------------------------------------

(defn partial-match? [GeocoderResult]
  (-> GeocoderResult (get 0) .-partial_match))

(defn formatted-address [GeocoderResult]
  (-> GeocoderResult (get 0) .-formatted_address))

(defn lat [GeocoderResult]
  (-> GeocoderResult (get 0) .-geometry .-location .lat))

(defn lng [GeocoderResult]
  (-> GeocoderResult (get 0) .-geometry .-location .lng))

(defn assoc-geocoder-request [row]
  (assoc row :address
   (str (when-let [route-type (:route_type row)]
          (str route-type " "))
        (when-let [route (:route row)]
          (str route ", "))
        (when-let [route-number (:route_number row)]
          (str route-number " - "))
        (when-let [neighborhood (:neighborhood row)]
          (str neighborhood ","))
        " Sinop - MT, Brasil")))

(defn extract-lat-lng [GeocoderResult crime-report]
  (let [sinop-coords (subscribe [:sinop])]
    ; save the result
    (dispatch
     [:geocode/add-result
          ; check if goog could geocoded the address partially only
      (if (and (partial-match? GeocoderResult)
               (= (lat GeocoderResult) (:lat @sinop-coords))
               (= (lng GeocoderResult) (:lng @sinop-coords)))
        ; default value in db is null; we must change it
        ; so that it won't be fetched again
        (merge crime-report
               {:latitude 0.0, :longitude 0.0})
        (merge crime-report
               {:latitude (lat GeocoderResult)
                :longitude (lng GeocoderResult)
                :found (formatted-address GeocoderResult)}))])))

(defn geocode
  "Use `js/google.maps.Geocoder` to geocode an address"
  [crime-report query-limit?]
  (let [geocoder (new js/google.maps.Geocoder)
        opts (clj->js (select-keys crime-report [:address]))
        handler (fn [GeocoderResult status]
                  (condp = status
                         "OK" (extract-lat-lng GeocoderResult crime-report)
                         "OVER_QUERY_LIMIT" (reset! query-limit? true)
                         "ZERO_RESULTS" (dispatch [:geocode/add-zero-results crime-report])
                         ;; default
                         (log status)))]
    (.geocode geocoder opts handler)))

(defn run-geocode
  "Applies geocode to each address given, making sure gmap's API
   policies are followed."
  ([crime-reports]
   (run-geocode crime-reports {:queries 0, :query-limit? (atom false)}))
  ([crime-reports {:keys [queries, start, query-limit?] :as control}]
   (cond
     @query-limit? (js/alert "OVER_QUERY_LIMIT")

     ;; NOTE: <implementatian simplicity> each 50 queries we wait
     ;;       for a second before continuing execution so we don't
     ;;       exceed our 50 queries/s limit.
     (= queries 50)
     (js/setTimeout
      #(run-geocode crime-reports (assoc control :queries 0))
      1000)

     (seq crime-reports)
     (do (geocode (assoc-geocoder-request (first crime-reports)) query-limit?)
         (run-geocode (rest crime-reports) (assoc control :queries (inc (:queries control)))))

     (empty? crime-reports)
     (js/alert "Fini!"))))

; NOTE: sample test data
(def addresses
  [{:address "Avenida Governador Júlio Campos, 1111 - Setor Comercial, Sinop - MT, Brasil"}
   {:address "Avenida das Itaúbas, 3777 - Setor Comercial, Sinop - MT, Brasil"}
   {:address "Rua Ouro Preto, 103 - Jardim Belo Horizonte, Sinop - MT, Brasil"}])


; ----------------------------------------------------------------------
; Events
; ----------------------------------------------------------------------

(reg-event-db
 :geocode/add-result
 (fn [db [_ result]]
   (update-in db [:geocode :result]
              conj result)))

(reg-event-db
 :geocode/add-zero-results
 (fn [db [_ crime-report]]
   (update-in db [:geocode :zero-results]
              conj crime-report)))

(reg-event-db
 :geocode
 (fn [db [_ crime-reports]]
   (run-geocode crime-reports)
   (assoc-in db [:geocode :ready?] true)))

(reg-event-db
 :geocode/reset-state
 (fn [db _]
   (-> db
       (assoc-in [:geocode :result] [])
       (assoc-in [:geocode :zero-results] [])
       (assoc-in [:geocode :ready?] false))))

(reg-event-db
 :geocode/sync-lat-lng
 (fn [db _]
   (ajax/GET "/db/crime-reports/geocode"
             {:handler #(do (dispatch-sync [:geocode/reset-state])
                            (dispatch [:geocode %]))
              :error-handler #(log %)})
   db))

(reg-event-db
 :update-crime-reports
 (fn [db [_ crime-reports]]
   (ajax/PUT "/api/crime-reports/update"
             {:handler #(do (dispatch [:geocoder/reset-state])
                            (js/alert "Base de Dados atualizada."))
              :error-handler #(log %)
              :params crime-reports})
   db))
