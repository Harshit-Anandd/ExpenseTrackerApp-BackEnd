-- Data initialization script for SpendSmart Auth Service
-- This file is executed after schema creation (Hibernate DDL)

-- Create default ADMIN user (password: admin123)
-- BCrypt hash for "admin123" with cost factor 12: $2a$12$kMZ2PnCMGx.yAr1CJVmx2eOEP7V6Q4h7KGR8wQzh8oq9/3mXZQTgC
INSERT INTO users (
    full_name, email, password_hash, currency, timezone,
    monthly_budget, provider, role, is_active, created_at, updated_at
) VALUES (
             'System Admin', 'admin@spendsmart.app',
             '$2a$12$kMZ2PnCMGx.yAr1CJVmx2eOEP7V6Q4h7KGR8wQzh8oq9/3mXZQTgC',
             'USD', 'UTC', 10000.0, 'LOCAL', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         );

-- Create test user (password: test123)
-- BCrypt hash for "test123" with cost factor 12: $2a$12$5h0Tl3yU.HU3xrX0M8K6BOYNlI8/KzJFj0w9Aq0l8Z7VF5T4XmLKa
INSERT INTO users (
    full_name, email, password_hash, currency, timezone,
    monthly_budget, provider, role, is_active,  created_at, updated_at
) VALUES (
             'Test User', 'test@spendsmart.app',
             '$2a$12$5h0Tl3yU.HU3xrX0M8K6BOYNlI8/KzJFj0w9Aq0l8Z7VF5T4XmLKa',
             'USD', 'UTC', 5000.0, 'LOCAL', 'USER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         );