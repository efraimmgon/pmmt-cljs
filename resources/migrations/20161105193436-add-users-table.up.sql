CREATE TABLE users
(id VARCHAR(20) PRIMARY KEY NOT NULL,
 first_name VARCHAR(30),
 last_name VARCHAR(30),
 email VARCHAR(30),
 admin BOOLEAN,
 last_login TIMESTAMP,
 is_active BOOLEAN,
 pass VARCHAR(300) NOT NULL);
 --;;
 -- create a system account for which the stock entries will be created
 INSERT INTO users (id, email, admin, is_active, pass)
 VALUES ('admin', 'admin@admin.com', TRUE, TRUE, 'bcrypt+sha512$86186fc28f83b3e3db78bcf8350a3a57$12$8f215420e68fd7922561167b07354f05d8db6d49e212689e');
