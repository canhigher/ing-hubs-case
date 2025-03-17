-- Customer 1 assets
INSERT INTO assets (customer_id, asset_name, size, usable_size) VALUES (1, 'TRY', 10000.00, 10000.00);
INSERT INTO assets (customer_id, asset_name, size, usable_size) VALUES (1, 'BTC', 1.5, 1.5);
INSERT INTO assets (customer_id, asset_name, size, usable_size) VALUES (1, 'ETH', 10.0, 10.0);

-- Customer 2 assets
INSERT INTO assets (customer_id, asset_name, size, usable_size) VALUES (2, 'TRY', 20000.00, 20000.00);
INSERT INTO assets (customer_id, asset_name, size, usable_size) VALUES (2, 'BTC', 0.5, 0.5);
INSERT INTO assets (customer_id, asset_name, size, usable_size) VALUES (2, 'ETH', 5.0, 5.0);
INSERT INTO assets (customer_id, asset_name, size, usable_size) VALUES (2, 'XRP', 1000.0, 1000.0);

-- Insert predefined roles
INSERT INTO roles (name) VALUES ('ROLE_CUSTOMER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');

-- After running the application for the first time, we can create an admin user with password 'admin' using the API, 
-- or you can uncomment the following line to add it directly to the database (not recommended for production)
-- INSERT INTO users (username, email, password) VALUES ('admin', 'admin@ingbrokerage.com', '$2a$10$ZNoHmZ4BrLA5jaYq3dlf8.UJA0NxQVIQHMA/MuZFomDRsZwpQ/Jfe');
-- INSERT INTO user_roles (user_id, role_id) VALUES (1, 2); 