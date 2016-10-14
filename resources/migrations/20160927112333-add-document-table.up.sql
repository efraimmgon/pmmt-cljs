-- 20160927112333
CREATE TABLE document (
  id serial PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  description TEXT,
  url VARCHAR(1000)
);
