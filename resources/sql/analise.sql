-- :name get-naturezas :? :*
-- retrive all `crimes` records
SELECT id, type FROM crimes

-- :name get-crime-reports-with-geo :? :*
-- :doc retrieves crimes for geolocalization (lat and lng not null)
SELECT * FROM crime_reports
  WHERE created_at BETWEEN :data_inicial AND :data_final
  AND latitude is NOT NULL
/*~ ; :crime_id
(cond
  (number? (:crime_id params)) "AND crime_id = :crime_id"
  (coll? (:crime_id params)) "AND crime_id IN :tuple:crime_id"
  :else nil)
~*/
--~ (when (:neighborhood params) "AND neighborhood LIKE :neighborhood")
--~ (when (:route params) "AND route LIKE :route")
/*~ ; :hora_inicial :hora_final
(when (and (:hora_inicial params) (:hora_final params))
  "AND created_on BETWEEN :hora_inicial AND :hora_final")
~*/

-- -------------------------------------------------------------------
-- CRIME REPORTS
-- -------------------------------------------------------------------


-- :name get-crime-reports :? :*
-- :doc fetches crime reports with usual filtering of fields
SELECT * FROM crime_reports AS cr
  INNER JOIN crimes AS c
  ON cr.crime_id = c.id
  WHERE cr.created_at BETWEEN :data-inicial AND :data-final
--~ (when (:neighborhood params) "AND cr.neighborhood LIKE :neighborhood")

-- :name get-crime-reports-raw :? :*
-- :doc fetches crime-reports with no filtering
SELECT * FROM crime_reports AS cr
  INNER JOIN crimes AS c
  ON cr.crime_id = c.id

-- :name get-crime-reports-count-raw :? :*
--:doc fetch crime-reports count with no filtering
SELECT COUNT(*) FROM crime_reports

-- :name get-crime-reports-count-by-crime-raw :? :*
SELECT c.type, COUNT(*) FROM crime_reports AS cr
  INNER JOIN crimes AS c
  ON cr.crime_id = c.id
  GROUP BY c.type ORDER BY c.type DESC

-- :name get-crime-reports-count :? :*
--:doc fetch crime-reports count with usual filtering
SELECT COUNT(*) FROM crime_reports AS cr
  WHERE cr.created_at BETWEEN :data-inicial AND :data-final
--~ (when (:neighborhood params) "AND cr.neighborhood LIKE :neighborhood")

-- :name get-crime-reports-count-by-crime
SELECT c.type, COUNT(*) FROM crime_reports AS cr
  INNER JOIN crimes AS c
  ON cr.crime_id = c.id
  WHERE cr.created_at BETWEEN :data-inicial AND :data-final
  AND cr.crime_id IN :tuple:crimes-id
--~ (when (:neighborhood params) "AND cr.neighborhood LIKE :neighborhood")
  GROUP BY c.type ORDER BY c.type DESC
