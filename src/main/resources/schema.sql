-- SpendSmart Auth Service Database Schema
-- MySQL 8.0+
-- This script creates the users table with proper indexes and constraints

-- Drop table if exists (for development/testing)
DROP TABLE IF EXISTS users;

-- Users table - Core authentication and user profile
CREATE TABLE users (
    -- Primary Key
                       user_id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Profile Information
                       full_name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255),  -- NULL for OAuth2-only users
                       avatar_url VARCHAR(2048),

    -- User Preferences
                       currency VARCHAR(10) NOT NULL DEFAULT 'USD',
                       timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
                       monthly_budget DOUBLE NOT NULL DEFAULT 5000.0,

    -- Authentication & Authorization
                       provider ENUM('LOCAL', 'GOOGLE') NOT NULL DEFAULT 'LOCAL',
                       role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',

    -- Account Status (soft delete)
                       is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit Timestamps (UTC)
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes for frequently accessed columns
                       INDEX idx_email (email),
                       INDEX idx_is_active (is_active),
                       INDEX idx_provider (provider),
                       INDEX idx_currency (currency),
                       INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
