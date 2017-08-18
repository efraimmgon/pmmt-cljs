-- GENERAL

-- -------------------------------------------------------------------
-- Reset DB
-- -------------------------------------------------------------------

-- :name delete-all-crimes! :! :n
DELETE FROM crimes

-- :name delete-all-crime-reports! !: :n
DELETE FROM crime_reports

-- :name delete-all-users! :! :n
DELETE FROM users

-- :name delete-all-modes-desc! :! :n
DELETE FROM modes_desc

-- :name delete-all-cities! :! :n
DELETE FROM cities

-- :name select-by-field :? :*
SELECT * FROM :i:table
  WHERE :i:field = :value

-- :name select-by-table :? :*
SELECT * FROM :i:table LIMIT 100

-- -------------------------------------------------------------------
-- USERS
-- -------------------------------------------------------------------

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, pass)
VALUES (:id, :pass)

-- :name get-user :? :1
-- :doc retrieve a user given the id.
SELECT * FROM users
WHERE id = :id

-- :name get-users :? :*
-- :doc retrieve a user given the id.
SELECT * FROM users

-- :name update-last-login :! :n
UPDATE users
 SET last_login = current_timestamp
 WHERE id = :id

-- :name delete-user! :! :n
-- :doc delete a user given the id
DELETE FROM users
WHERE id = :id

-- -------------------------------------------------------------------
-- CRIMES
-- -------------------------------------------------------------------

-- :name get-crimes :? :*
-- retrieve all crime records
SELECT id, type FROM crimes

-- :name create-crime! :! :n
-- create a `crime` record
INSERT INTO crimes (type) VALUES (:type)

-- :name create-crime-with-id! :! :n
-- create a `crime` record with id
INSERT INTO crimes (id, type) VALUES (:id, :type)

-- -------------------------------------------------------------------
-- CRIME REPORTS
-- -------------------------------------------------------------------

-- :name create-crime-report! :! :n
-- create a `crime-report` record
INSERT INTO crime_reports
(report_number, report, crime_id, mode_desc_id, city_id, neighborhood,
 route_type, route, route_number, created_at, created_on)
VALUES
(:report_number, :report, :crime_id, :mode_desc_id, :city_id, :neighborhood,
 :route_type, :route, :route_number, :created_at, :created_on)

-- :name crime-reports-with-null-coordinates :? :*
-- fetch `crime-reports` records with `latitude` and `longitude` = null
SELECT * FROM crime_reports
 WHERE latitude = NULL AND longitude = NULL

 -- -------------------------------------------------------------------
 -- MODES DESC
 -- -------------------------------------------------------------------

-- :name get-modes-desc :? :*
SELECT * FROM modes_desc

-- :name create-mode-desc! :! :n
INSERT INTO modes_desc
  (type) VALUES (:type)

-- -------------------------------------------------------------------
-- CITIES
-- -------------------------------------------------------------------

-- :name get-cities :? :*
SELECT * FROM cities

-- :name create-city! :! :n
INSERT INTO cities
  (name) VALUES (:name)
