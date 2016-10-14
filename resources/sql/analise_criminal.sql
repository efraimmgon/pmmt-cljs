-- :name create-city! :! :n
-- create a `cidade` record
INSERT INTO cidade (nome) VALUES (:nome)

-- :name create-natureza! :! :n
-- create a `natureza` record
INSERT INTO natureza (nome) VALUES (:nome)

-- :name create-ocorrencia! :! :n
-- create a `ocorrencia` record
INSERT INTO ocorrencia
(data, cidade_id, local, bairro, via, numero,
 latitude, longitude, natureza_id, hora, periodo)
VALUES (:data, :cidade_id, :local, :bairro, :via, :numero,
 :latitude, :longitude, :natureza_id, :hora, :periodo)

-- :name create-tag! :! :n
-- create a `tag` record
INSERT INTO tag
(name, description)
VALUES (:name, :description)

-- :name create-document! :! :n
-- create a `document` record
INSERT INTO document
(name, description, url)
VALUES (:name, :description, :url)

-- :name create-tag-document! :! :n
-- create a `tag_document` record
INSERT INTO tag_document
(tag_id, doc_id)
VALUES (:tag_id, :doc_id)

-- :name get-geo-data :? :*
SELECT * FROM ocorrencia
  WHERE data BETWEEN :data_inicial AND :data_final
  AND cidade_id = :cidade
  AND latitude is NOT NULL
  AND natureza_id = :natureza
