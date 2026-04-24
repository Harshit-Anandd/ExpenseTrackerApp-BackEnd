# SpendSmart Authentication Service

## Overview
The SpendSmart Authentication Service is a robust, secure, and scalable microservice responsible for managing user identities, authentication, authorization, and profile management within the broader SpendSmart platform. It provides both traditional local authentication (email/password) and OAuth2 social login (Google) utilizing a stateless JWT-based architecture.

## Core Features
*   **User Registration & Authentication:** Secure email and password registration, along with login functionality returning JWT access and refresh tokens.
*   **OAuth2 Integration:** Seamless social login capabilities using Google as the OAuth2 provider.
*   **Stateless Security:** Implementation of JWT (JSON Web Tokens) for stateless session management, allowing horizontal scalability.
*   **Role-Based Access Control (RBAC):** Distinct roles (e.g., `USER`, `ADMIN`) governing access to specific endpoints.
*   **Two-Factor Authentication (2FA) & OTP:** Support for One-Time Passwords (OTP) and two-factor authentication toggling to enhance security.
*   **Token Lifecycle Management:** Implementation of token refreshing mechanisms and token blocklisting (for secure logout).
*   **Profile & Account Management:** Endpoints for profile updates (avatar, currency preference, timezone), password changes, and account soft-deletion (deactivation).
*   **Event-Driven Notifications:** Integration with RabbitMQ to publish asynchronous authentication and user lifecycle events to other microservices.
*   **Service Discovery:** Registered as a Eureka client for easy discovery by API Gateways and other microservices within the SpendSmart ecosystem.

## Tech Stack & Justifications

*   **Java 17:** Selected as the core programming language for its robust ecosystem, strong typing, and the inclusion of modern features (like records and pattern matching) in an LTS (Long Term Support) release.
*   **Spring Boot 3.3.5:** Chosen for rapid application development. Its opinionated auto-configuration eliminates immense amounts of boilerplate, speeding up the development of RESTful web services.
*   **Spring Security & Spring Boot Starter OAuth2 Client:** The industry standard for securing Spring applications. Used for its comprehensive protection against common vulnerabilities (though CSRF is disabled here in favor of stateless JWTs), and its built-in, easily configurable support for OAuth2 flows.
*   **JJWT (Java JWT):** A reliable library used to securely construct and verify JSON Web Tokens, facilitating the stateless architecture.
*   **Spring Data JPA & Hibernate:** Provides an abstraction over JDBC, drastically reducing the amount of boilerplate SQL required. Used to map Java objects (Entities) directly to database tables.
*   **MySQL 8 (via `mysql-connector-j`):** A battle-tested, ACID-compliant relational database. Chosen over NoSQL to strictly enforce relational integrity and constraints on core user identity data.
*   **Redis:** An in-memory data structure store used for highly performant, ephemeral data storage. In this project, it is primarily utilized to handle OTP (One-Time Password) challenges (`RedisOtpChallengeService`) and likely token blocklisting, where rapid read/write operations and TTL (Time-To-Live) expirations are critical.
*   **RabbitMQ:** An AMQP message broker used to decouple the authentication service from notification or auditing services. Emitting events (like `AuthNotificationEvent`) ensures that heavy tasks like sending welcome emails don't block the immediate authentication response.
*   **Spring Cloud Netflix Eureka:** Acts as a service registry. In a microservices architecture, this allows the API Gateway or other services to dynamically locate the Auth Service instances without hardcoded IP addresses.
*   **Lombok:** A compile-time tool used to minimize boilerplate code (Getters, Setters, Constructors, Builders) keeping domain classes clean and readable.
*   **MapStruct:** A code generator that simplifies mapping between complex object types (e.g., `User` Entity to `UserProfileDto`). Selected over Reflection-based mappers (like ModelMapper) for its superior performance and compile-time type safety.
*   **Springdoc OpenAPI:** Automatically generates interactive API documentation (Swagger UI) from code annotations, ensuring that API consumers always have up-to-date specifications.

## Architectural Design Decisions

1.  **Stateless JWT Authentication:**
    We moved away from traditional session-based authentication to a stateless approach using JWTs. This eliminates the need for sticky sessions or shared session stores across auth-service instances, making the service easily scalable. Refresh tokens (with a 7-day lifespan) provide a seamless user experience while access tokens remain short-lived (24 hours).
2.  **Soft-Deletion (Deactivation):**
    The `User` entity utilizes an `isActive` flag. Instead of permanently deleting records, accounts are soft-deleted. This preserves referential integrity in downstream services (e.g., transaction histories tied to a user ID) while preventing the deactivated user from logging in.
3.  **Event-Driven Decoupling:**
    Rather than directly calling an email service synchronously when a user registers, the service publishes an event to a RabbitMQ exchange (`spendsmart.auth.events`). This ensures high availability and fast response times for the client, while offloading the notification logic.
4.  **Security Filter Chain Ordering:**
    A custom `JwtAuthenticationFilter` is strictly ordered before the standard `UsernamePasswordAuthenticationFilter`. This ensures that every request is intercepted and validated for a valid JWT before it hits any authorization logic or controller endpoints.
5.  **Multi-Provider Support Strategy:**
    The database schema gracefully handles both `LOCAL` and `GOOGLE` provider types. The `passwordHash` column is purposefully made nullable to cleanly support OAuth2 users who do not have a traditional password on our system.

## Project Structure Overview
*   `com.spendsmart.auth.config`: Configuration classes for OpenAPI and RabbitMQ.
*   `com.spendsmart.auth.controller`: REST APIs for authentication, profile management, and admin duties.
*   `com.spendsmart.auth.dto`: Data Transfer Objects isolating internal domain models from API consumers.
*   `com.spendsmart.auth.entity`: The core persistence model (e.g., `User`).
*   `com.spendsmart.auth.exception`: Global exception handling (`@ControllerAdvice`) ensuring uniform error responses.
*   `com.spendsmart.auth.messaging`: RabbitMQ publishers and event structures.
*   `com.spendsmart.auth.repository`: Spring Data JPA interfaces for database interaction.
*   `com.spendsmart.auth.security`: JWT utilities, filters, and OAuth2 success/failure handlers.
*   `com.spendsmart.auth.service`: Core business logic interfaces and implementations.

## How to Run locally

### Prerequisites
*   Java 17
*   Maven
*   MySQL 8 instance running on `localhost:3306` (Schema: `spendsmart_auth`)
*   Redis running on `localhost:6379`
*   RabbitMQ running on `localhost:5672`
*   Eureka Server running on `http://localhost:8761/eureka/` (optional, for full ecosystem testing)

### Steps
1. Navigate to the project root directory.
2. Ensure you have the required environment variables or adjust the defaults in `application.yml`.
3. Build the project:
   ```bash
   ./mvnw clean install
   ```
4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
5. Access Swagger API documentation at: `http://localhost:8081/swagger-ui.html`
