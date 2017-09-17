-- :name get-crimes :? :*
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
  WHERE cr.created_at BETWEEN :from AND :to
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
  WHERE cr.created_at BETWEEN :from AND :to
--~ (when (:neighborhood params) "AND cr.neighborhood LIKE :neighborhood")


-- :name get-crime-reports-by :? :*
-- :doc crime_reports grouped by :field. Is filtered by :from and :to DATES
SELECT :i:field,
       COUNT(*) FROM crime_reports cr
  JOIN crimes c ON cr.crime_id = c.id
  WHERE cr.created_at BETWEEN :from AND :to
    AND :i:field IS NOT NULL
  GROUP BY 1
  ORDER BY 2 DESC
--~ (when (:limit params) "LIMIT :limit")


-- :name get-crime-reports-by-route :? :*
-- :doc crime_reports grouped by `route`, filtered by :from and :to DATES
SELECT cr.route_type || ' ' || cr.route AS "route",
       COUNT(*) FROM crime_reports cr
  JOIN crimes c ON cr.crime_id = c.id
  WHERE cr.created_at BETWEEN :from AND :to
    AND cr.route IS NOT NULL
  GROUP BY 1
  ORDER BY 2 DESC
--~ (when (:limit params) "LIMIT :limit")


-- :name get-crime-reports-by-weekday :? :*
-- :doc crime_reports grouped by weekday, filtered by :from and :to (DATES)
SELECT EXTRACT(dow FROM created_at) AS "weekday",
       COUNT(*)
  FROM crime_reports cr
  JOIN crimes c ON cr.crime_id = c.id
  WHERE cr.created_at BETWEEN :from AND :to
  GROUP BY 1
  ORDER BY 1
--~ (when (:limit params) "LIMIT :limit")


-- :name get-crime-reports-count-by-crime
SELECT c.type AS "crime_type",
       COUNT(*)
  FROM crime_reports AS cr
  INNER JOIN crimes AS c
  ON cr.crime_id = c.id
  WHERE cr.created_at BETWEEN :from AND :to
  AND cr.crime_id IN :tuple:crimes-id
--~ (when (:neighborhood params) "AND cr.neighborhood LIKE :neighborhood")
  GROUP BY c.type ORDER BY c.type DESC


-- :name get-crime-reports-by-crime-type :? :*
-- :doc crime_reports grouped by crime type; filtered by :from and :to DATES
SELECT c.type AS "crime-type",
       COUNT(*)
  FROM crime_reports cr
  JOIN crimes c ON cr.crime_id = c.id
  WHERE cr.created_at BETWEEN :from AND :to
  GROUP BY 1
  ORDER BY 2 DESC

-- :name get-crime-reports-by-neighborhood :? :*
-- :doc crime_reports grouped by neighborhood; filtered by :from and :to DATES
SELECT c.neighborhood
       COUNT(*)
  FROM crime_reports cr
  JOIN crimes c ON cr.crime_id = c.id
  WHERE cr.created_at BETWEEN :from AND :to
  GROUP BY 1
  ORDER BY 2 DESC


-- :name get-crime-reports-by-month
-- :doc crime_reports by month
SELECT DATE_TRUNC('month', created_at) as month,
       COUNT(*)
  FROM crime_reports cr
  WHERE cr.created_at BETWEEN :from AND :to
  GROUP BY 1
  ORDER BY 1


-- :name get-crime-reports-by-period
-- :doc crime_reports grouped by 6 hour periods (00:00 - 05:59 ...),
-- filtered by :from and :to (DATES)
SELECT TRUNC(EXTRACT(hour FROM cr.created_on) / 6) AS "period",
       COUNT(*)
  FROM crime_reports cr
  WHERE cr.created_on IS NOT NULL
    AND cr.created_at BETWEEN :from AND :to
  GROUP BY 1
  ORDER BY 1


-- :name get-crime-reports-by-hour
-- :doc crime_reports grouped by hour
SELECT EXTRACT(hour FROM cr.created_on) AS "hour",
       COUNT(*)
  FROM crime_reports cr
  WHERE cr.created_on IS NOT NULL
    AND cr.created_at BETWEEN :from AND :to
  GROUP BY 1
  ORDER BY 1


-- :name get-crime-reports-by-date
-- :doc crime_reports grouped by `created_at`, filtered by :from and :to (DATES)
SELECT cr.created_at AS "created-at",
       COUNT(*)
  FROM crime_reports cr
  WHERE cr.created_at BETWEEN :from AND :to
  GROUP BY 1
  ORDER BY 1
