-- GENERAL

-- :name select-by-field
SELECT * FROM :i:table
  WHERE :i:field = :value

-- :name select-by-table
SELECT * FROM :i:table LIMIT 100


-- USERS

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, pass)
VALUES (:id, :pass)

-- :name get-user :? :1
-- :doc retrieve a user given the id.
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc delete a user given the id
DELETE FROM users
WHERE id = :id

-- OCORRENCIA

-- :name get-reports :? :*
-- retrieve all `ocorrencia` records
SELECT * FROM ocorrencia


-- CIDADE

-- :name get-cities :? :*
-- retrieve all `cidade` records
SELECT id, nome FROM cidade

-- NATUREZA

-- :name get-incidents :? :*
-- retrieve all `natureza` records
SELECT id, nome  FROM natureza

-- TAG

-- :name get-tags :? :*
-- retrieve all `tag` records
SELECT id, name, description FROM tag

-- DOCUMENT

-- :name get-documents :? :*
-- retrieve all `document` records
SELECT id, name, description FROM document

-- TAG_DOCUMENT

-- :name get-documents-and-tags-id :? :*
-- retrieve all documents with their respective tags id
SELECT * FROM tag_document
INNER JOIN document
ON document.id = tag_document.doc_id

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
