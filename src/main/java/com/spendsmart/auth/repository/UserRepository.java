/**
 * UserRepository provides data access operations for the User entity.
 *
 * <p>This repository extends JpaRepository to leverage Spring Data JPA's built-in CRUD
 * functionality while adding custom query methods specific to user lookups and filters.
 *
 * <p>All methods are non-blocking and execute within the context of a database transaction.
 * The repository is automatically injected via @Autowired or constructor injection.
 *
 * @author SpendSmart Auth Team
 * @version 1.0
 */
package com.spendsmart.auth.repository;

import com.spendsmart.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     *
     * @param email the email to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their unique userId.
     *
     * @param userId the primary key to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUserId(Long userId);

    /**
     * Check if a user with the given email already exists.
     * Efficient for duplicate email validation before registration.
     *
     * @param email the email to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all users with a specific active/inactive status.
     * Useful for statistics and account management operations.
     *
     * @param isActive true to find active users, false for inactive
     * @return List of users matching the status
     */
    List<User> findByIsActive(boolean isActive);

    /**
     * Find all users with a specific currency preference.
     * Useful for bulk notification or data export operations.
     *
     * @param currency the currency code (e.g., "USD")
     * @return List of users with the specified currency
     */
    List<User> findByCurrency(String currency);

    /**
     * Count the number of active or inactive users.
     * Useful for analytics and reporting.
     *
     * @param isActive true to count active users, false for inactive
     * @return the count of users with the specified status
     */
    long countByIsActive(boolean isActive);

    /**
     * Delete a user by their userId.
     * Note: This is a hard delete (not recommended in production).
     * Prefer using updateProfile with isActive=false for soft delete.
     *
     * @param userId the primary key of the user to delete
     */
    void deleteByUserId(Long userId);
}

