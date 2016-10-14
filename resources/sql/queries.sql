-- :name get-tags :? :*
-- retrieve all `tag` records
SELECT * FROM tag

-- :name get-documents-and-tags-id :? :*
-- retrieve all documents with their respective tags id
SELECT * FROM tag_document
INNER JOIN document
ON document.id = tag_document.doc_id

-- :name get-cities :? :*
-- retrieve all `cidade` records
SELECT id, nome FROM cidade

-- :name get-naturezas :? :*
-- retrive all `natureza` records
SELECT id, nome FROM natureza

-- :name sample-ocorrencias :? :*
SELECT * FROM ocorrencia WHERE latitude IS NOT NULL LIMIT 5
