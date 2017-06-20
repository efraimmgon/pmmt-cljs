-- :name get-naturezas :? :*
-- retrive all `natureza` records
SELECT id, nome FROM natureza

-- :name get-ocorrencias-with-geo :? :*
-- :doc retrieves ocorrencias for geolocalization (lat and lng not null)
SELECT * FROM ocorrencia
  WHERE data BETWEEN :data_inicial AND :data_final
  AND latitude is NOT NULL
/*~ ; :natureza_id
(cond
  (number? (:natureza_id params)) "AND natureza_id = :natureza_id"
  (coll? (:natureza_id params)) "AND natureza_id IN :tuple:natureza_id"
  :else nil)
~*/
--~ (when (:bairro params) "AND bairro LIKE :bairro")
--~ (when (:via params) "AND via LIKE :via")
/*~ ; :hora_inicial :hora_final
(when (and (:hora_inicial params) (:hora_final params))
  "AND hora BETWEEN :hora_inicial AND :hora_final")
~*/

-- REPORT ----------------------------------------------------------


-- :name get-reports :? :*
-- :doc fetches reports with usual filtering of fields
SELECT * FROM ocorrencia AS o
  INNER JOIN natureza AS n
  ON o.natureza_id = n.id
  WHERE o.data BETWEEN :data-inicial AND :data-final
--~ (when (:bairro params) "AND o.bairro LIKE :bairro")

-- :name get-reports-raw :? :*
-- :doc fetches reports with no filtering
SELECT * FROM ocorrencia AS o
  INNER JOIN natureza AS n
  ON o.natureza_id = n.id

-- :name get-reports-count-raw :? :*
--:doc fetch reports count with no filtering
SELECT COUNT(*) FROM ocorrencia

-- :name get-reports-count-by-offense-raw :? :*
SELECT n.nome, COUNT(*) FROM ocorrencia AS o
  INNER JOIN natureza AS n
  ON o.natureza_id = n.id
  GROUP BY n.nome ORDER BY n.nome DESC

-- :name get-reports-count :? :*
--:doc fetch reports count with usual filtering
SELECT COUNT(*) FROM ocorrencia AS o
  WHERE o.data BETWEEN :data-inicial AND :data-final
--~ (when (:bairro params) "AND o.bairro LIKE :bairro")

-- :name get-reports-count-by-offense :? :*
SELECT n.nome, COUNT(*) FROM ocorrencia AS o
  INNER JOIN natureza AS n
  ON o.natureza_id = n.id
  WHERE o.data BETWEEN :data-inicial AND :data-final
  AND o.natureza_id IN :tuple:natureza-id
--~ (when (:bairro params) "AND o.bairro LIKE :bairro")
  GROUP BY n.nome ORDER BY n.nome DESC
