-- 20160927112824
CREATE TABLE tag_document (
  id serial PRIMARY KEY,
  tag_id INT NOT NULL REFERENCES tag (id),
  doc_id INT NOT NULL REFERENCES document (id),
  UNIQUE (tag_id, doc_id)
);
