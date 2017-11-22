(ns pmmt.utils.geocoder)

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
    - address => a String
    - #{:ok :query-limit :zero-results}
        > handlers
        > Take a GeocoderResult as arg"
  [{:keys [address ok query-limit zero-results]}]
  (let [geocoder (new js/google.maps.Geocoder)
        opts (clj->js {:address address})
        query-limit (or query-limit #(println "OVER_QUERY_LIMIT"))
        zero-results (or zero-results #(println "ZERO RESULTS for: " address))
        handler (fn [GeocoderResult status]
                  (condp = status
                         "OK" (ok GeocoderResult)
                         "OVER_QUERY_LIMIT" (query-limit GeocoderResult)
                         "ZERO_RESULTS" (zero-results GeocoderResult)
                         ;; default
                         (println "DEFAULT => " status)))]
    (.geocode geocoder opts handler)))

(defn geocode-addresses
  "Applies geocode-address to each address given, making sure gmap's API
   policies are followed.
  args:
  - addresses => a vector of strings
  - handlers =>
      > handlers for #{:done :ok :query-limit :zero-results}
      > #{:ok :query-limit :zero-results} => take a GeocoderResult as arg"
  [addresses {:keys [done ok query-limit zero-results] :as handlers}]
  (let [done (or done #(js/alert "Done geocoding."))
        query-limit (or query-limit #(println "OVER_QUERY_LIMIT"))
        f (fn f [addresses queries query-limit?]
            (cond
              ; js/google.maps.Geocoder returned OVER_QUERY_LIMIT
              @query-limit? (query-limit)

              ; NOTE: <implementatian simplicity> each `n` queries we wait
              ;       for 5 seconds before continuing execution so we don't
              ;       exceed our 50 queries/s limit.
              (= queries 20)
              (js/setTimeout
               #(f addresses 0 query-limit?))

              ; still addresses left to geocode?
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
