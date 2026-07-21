-- Seed Mock User (password_hash is BCrypt for 'SecurePassword123')
INSERT INTO users (id, username, email, password_hash, created_at, updated_at)
VALUES (
    'e8a4d46c-1798-4b72-88d4-f6515f40192a', 
    'johndoe', 
    'john@example.com', 
    '$2a$10$R9hGeEVd908pqvgQ.g3S5Oe1tXh5sYfR6pQe3j9Y5pWz2wF7zDxeO', 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
);

-- Seed Mock Table Definition
INSERT INTO table_definitions (table_id, user_id, table_name, schema_json, created_at, updated_at)
VALUES (
    'bc7279fb-f203-4b68-b76b-90f779782ea5', 
    'e8a4d46c-1798-4b72-88d4-f6515f40192a', 
    'Inventory Metrics', 
    '{"columns": [{"name": "Item Code", "type": "string"}, {"name": "Cost", "type": "decimal"}]}'::jsonb, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
);

-- Seed Mock Records
INSERT INTO records (record_id, user_id, table_id, data, created_at, updated_at)
VALUES 
(
    '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d', 
    'e8a4d46c-1798-4b72-88d4-f6515f40192a', 
    'bc7279fb-f203-4b68-b76b-90f779782ea5', 
    '{"Item Code": "A102", "Cost": 299.50}'::jsonb, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
),
(
    '8c2deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6e', 
    'e8a4d46c-1798-4b72-88d4-f6515f40192a', 
    'bc7279fb-f203-4b68-b76b-90f779782ea5', 
    '{"Item Code": "B504", "Cost": 19.99}'::jsonb, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
);
