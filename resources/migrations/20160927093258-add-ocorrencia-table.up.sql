-- 20160927093258
CREATE TABLE ocorrencia (
  id serial PRIMARY KEY,
  data BIGINT,
  cidade_id INT REFERENCES cidade (id),
  local VARCHAR(500),
  bairro VARCHAR(200),
  via VARCHAR(200),
  numero VARCHAR(100),
  latitude REAL,
  longitude REAL,
  natureza_id INT REFERENCES  natureza (id),
  hora BIGINT,
  periodo VARCHAR(200)
);
