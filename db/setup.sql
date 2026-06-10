-- One-time MySQL bootstrap for the Wordle app (NOT a Flyway migration).
-- Run as root:  mysql -u root -p < db/setup.sql
-- Replace CHANGE_ME with the password you put in
-- C:\WORDLE-secrets\secrets.properties (spring.datasource.password).

CREATE DATABASE IF NOT EXISTS wordle_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS wordle_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'wordle'@'localhost' IDENTIFIED BY 'CHANGE_ME';
GRANT ALL PRIVILEGES ON wordle_test.* TO 'wordle'@'localhost';
GRANT ALL PRIVILEGES ON wordle_prod.* TO 'wordle'@'localhost';
FLUSH PRIVILEGES;
