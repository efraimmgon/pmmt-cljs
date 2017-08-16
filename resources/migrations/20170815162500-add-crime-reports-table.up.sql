CREATE TABLE crime_reports (
  id serial PRIMARY KEY,
  report_number TEXT,
  report TEXT,
  crime_id INT REFERENCES crimes (id),
  mode_desc_id INT REFERENCES modes_desc (id),
  city_id INT REFERENCES cities (id),
  neighborhood TEXT,
  route_type TEXT,
  route TEXT,
  route_number TEXT,
  route_complement TEXT,
  latitude REAL,
  longitude REAL,
  created_at DATE,
  created_on TIME
);
