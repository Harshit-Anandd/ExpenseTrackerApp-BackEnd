/**
 * User entity representing a registered user in the SpendSmart platform.
 *
 * <p>This entity encapsulates all user-related data including authentication credentials,
 * profile information, preferences, and account status. It serves as the core domain object
 * for the auth-service and is persisted in the MySQL database.
 *
 * <p>Key Design Decisions:
 * - Uses Long as the primary key (auto-increment) for simplicity and database efficiency.
 * - Email is unique and indexed as the primary login identifier.
 * - Password is never exposed in DTOs or API responses.
 * - Provider (LOCAL or GOOGLE) determines the authentication method and influences password handling.
 * - isActive enables soft-delete functionality without losing historical data.
 * - Timestamps track creation and updates for audit trails.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_is_active", columnList = "is_active"),
        @Index(name = "idx_provider", columnList = "provider"),
        @Index(name = "idx_currency", columnList = "currency")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Unique identifier for the user (Primary Key).
     * Auto-incremented by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    /**
     * Full name of the user.
     * Required field.
     */
    @Column(nullable = false, length = 255)
    private String fullName;

    /**
     * Email address of the user.
     * Unique identifier used for login.
     * Required and indexed for fast lookups.
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Hashed password using BCrypt.
     * Never exposed in DTOs or API responses.
     * Nullable to support OAuth2-only accounts (provider = GOOGLE).
     */
    @Column(length = 255)
    private String passwordHash;

    /**
     * User's preferred display currency (ISO 4217 code, e.g., "USD", "EUR").
     * Default: "USD"
     */
    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    /**
     * User's timezone (IANA timezone identifier, e.g., "America/New_York").
     * Default: "UTC"
     */
    @Column(length = 50)
    @Builder.Default
    private String timezone = "UTC";

    /**
     * URL to the user's avatar image.
     * Nullable if user hasn't uploaded a profile picture.
     */
    @Column(length = 2048)
    private String avatarUrl;

    /**
     * Provider-specific ID (e.g., Google user ID).
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    /**
     * Subscription type (NORMAL = free tier, PAID = premium features).
     */
    @Column(name = "subscription_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionType subscriptionType = SubscriptionType.NORMAL;

    /**
     * Authentication provider (LOCAL for email/password, GOOGLE for OAuth2).
     * Determines whether the user has a local password or authenticates via OAuth2.
     */
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Provider provider = Provider.LOCAL;

    /**
     * User's role in the system (USER or ADMIN).
     * Used for role-based access control (RBAC).
     */
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Account activation status.
     * false indicates a soft-deleted account (user cannot log in).
     * Default: true
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    /**
     * Account creation timestamp (UTC).
     * Set automatically by Hibernate.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Account last update timestamp (UTC).
     * Updated automatically by Hibernate on every modification.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;



    /**
     * Enumeration for authentication providers.
     * Determines how the user authenticates (local credentials or OAuth2).
     */
    public enum Provider {
        LOCAL,      // Traditional email/password authentication
        GOOGLE      // OAuth2 authentication via Google
    }

    /**
     * Enumeration for user roles in the system.
     * Supports role-based access control.
     */
    public enum Role {
        USER,       // Regular user with standard permissions
        ADMIN       // Administrator with elevated permissions
    }

    /**
     * Subscription tiers for the platform.
     */
    public enum SubscriptionType {
        NORMAL,     // Free tier with limited features
        PAID        // Premium tier with all features
    }
}

