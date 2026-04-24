-- Data initialization script for SpendSmart Auth Service
-- This file is executed after schema creation (Hibernate DDL)

-- Create default ADMIN user (password: admin123)
-- BCrypt hash for "admin123" with cost factor 12: $2a$12$7YuBaH.7GtiKFnUEOWoKhOTLMBtCpOHL/YcW1o1hf7oiVK70LxMbO
INSERT INTO users (
    full_name, email, password_hash, currency, timezone,
    monthly_budget, provider, role, is_active, created_at, updated_at
) VALUES (
             'System Admin', 'admin@spendsmart.app',
             '$2a$12$7YuBaH.7GtiKFnUEOWoKhOTLMBtCpOHL/YcW1o1hf7oiVK70LxMbO',
             'USD', 'UTC', 10000.0, 'LOCAL', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         );

-- Create test user (password: test123)
-- BCrypt hash for "test123" with cost factor 12: $2a$12$pLPhd0pobawPcOV.W4gzk.aFNBc9/UO9E7Xe2QceD/VdMjxFd9YIW
INSERT INTO users (
    full_name, email, password_hash, currency, timezone,
    monthly_budget, provider, role, is_active,  created_at, updated_at
) VALUES (
             'Test User', 'test@spendsmart.app',
             '$2a$12$pLPhd0pobawPcOV.W4gzk.aFNBc9/UO9E7Xe2QceD/VdMjxFd9YIW',
             'USD', 'UTC', 5000.0, 'LOCAL', 'USER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         );