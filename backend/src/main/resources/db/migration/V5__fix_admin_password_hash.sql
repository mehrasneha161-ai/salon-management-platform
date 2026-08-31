-- ============================================================
-- V5 — Repair the seeded Super Admin password hash
-- ============================================================
-- The V1 seed stored a bcrypt hash that does not correspond to the documented
-- password "Admin@123", so admin login failed while runtime-registered
-- customers worked. V1 cannot be edited (its Flyway checksum is already
-- applied on existing databases), so this migration repairs the hash in place.
--
-- Hash below is BCrypt (cost 12, $2a$ variant emitted by Spring Security's
-- BCryptPasswordEncoder) and verifies the password "Admin@123".
UPDATE users
SET password_hash = '$2a$12$4qNNG/59M.//xt7DHzpHQO/iqCYIkP0QjT7uQZA6nqhbx/8n5CeZ6'
WHERE phone_number = '9999999999'
  AND role = 'ADMIN';
