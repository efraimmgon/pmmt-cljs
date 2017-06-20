-- 20160927093258
CREATE TABLE ocorrencia (
  id serial PRIMARY KEY,
  data DATE,
  bairro VARCHAR(200),
  via VARCHAR(200),
  numero VARCHAR(100),
  latitude REAL,
  longitude REAL,
  natureza_id INT REFERENCES natureza (id) ON DELETE CASCADE,
  hora TIME
);
