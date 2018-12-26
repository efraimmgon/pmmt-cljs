(ns pmmt.utils.geocoder
  (:require
   [clojure.string :as string]))

; JS Dependencies:
; - js/google.maps.Geocoder

; ------------------------------------------------------------------------------
; Helpers
; ------------------------------------------------------------------------------

(defn formatted-address [GeocoderResult]
  (-> GeocoderResult (get 0) .-formatted_address))

(defn lat [GeocoderResult]
  (-> GeocoderResult (get 0) .-geometry .-location .lat))

(defn lng [GeocoderResult]
  (-> GeocoderResult (get 0) .-geometry .-location .lng))

(defn join-address-components [sep coll]
  (->> coll
       (remove string/blank?)
       (string/join sep)))

(defn geocoder-request-address
  [{:keys [route-type route route-number neighborhood
           city state country zip-code]}]
  (let [one (join-address-components " " [route-type, route])
        two (join-address-components ", "
             [route-number
              neighborhood
              city
              state
              country
              zip-code])]
    (string/join ", " [one two])))


; ------------------------------------------------------------------------------
; Core
; ------------------------------------------------------------------------------

; What does js/google.maps.Geocoder requires?
; - A string, representing an address, containing a route, a neighborhood,
; a state, and a country.
; - a handler for what to do with the result.
; There are three possible results:
; > OK
; > OVER_QUERY_LIMIT
; > ZERO_RESULTS
; Only the first one matters for us.

; So it seems I need only to provide the addresses and a handler.
; As for the rest, some sensible defaults should suffice.

(defn geocode-address
  "Uses `js/google.maps.Geocoder` to geocode an address
    args:
    - address => a map with keys:
        > #{route-type route route-number neighborhood
            city state country zip-code}
    - #{:ok :query-limit :zero-results}
        > handlers
        > Take a GeocoderResult as arg"
  [{:keys [address ok query-limit zero-results]}]
  (let [geocoder (new js/google.maps.Geocoder)
        formatted-address (geocoder-request-address address)
        opts (clj->js {:address formatted-address})
        query-limit (or query-limit #(println "OVER_QUERY_LIMIT"))
        zero-results (or zero-results #(println "ZERO RESULTS for: " formatted-address))
        handler (fn [GeocoderResult status]
                  (case status
                    "OK" (ok GeocoderResult address)
                    "OVER_QUERY_LIMIT" (query-limit GeocoderResult)
                    "ZERO_RESULTS" (zero-results GeocoderResult)
                    ;; default
                    (println "DEFAULT => " status)))]
    (.geocode geocoder opts handler)))

(defn geocode-addresses
  "Applies geocode-address to each address given, making sure gmap's API
   policies are followed.
  args:
  - addresses => a vector of maps according to `geocode-address` requirements
  - handlers =>
      > handlers for #{:done :ok :query-limit :zero-results}
      > #{:ok :query-limit :zero-results} => take a GeocoderResult as arg"
  [addresses {:keys [done ok query-limit zero-results] :as handlers}]
  (let [done (or done #(js/alert "Done geocoding."))
        query-limit (or query-limit #(println "OVER_QUERY_LIMIT"))
        f (fn f [addresses queries query-limit?]
            (cond
              ;; js/google.maps.Geocoder returned OVER_QUERY_LIMIT
              @query-limit? (query-limit)

              ;; NOTE: <implementatian simplicity> each `n` queries we wait
              ;;       for 5 seconds before continuing execution so we don't
              ;;       exceed our 50 queries/s limit.
              (= queries 10)
              (js/setTimeout
               #(f addresses 0 query-limit?)
               20000)

              ;; still addresses left to geocode?
              (seq addresses)
              (do (geocode-address {:address (first addresses)
                                    :ok ok
                                    :query-limit #(do (query-limit %)
                                                      (reset! query-limit? true))
                                    :zero-results zero-results})
                  (f (rest addresses) (inc queries) query-limit?))

              ; no more addresses left
              (empty? addresses)
              (done)))]
    (f addresses 0 (atom false))))
