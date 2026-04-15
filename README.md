# SpendSmart Auth Service

**A production-ready, microservices-based authentication service for the SpendSmart personal finance platform.**

## Overview

The Auth Service is the central authentication provider for SpendSmart, handling:
- User registration and login
- JWT token generation and validation
- Google OAuth2 integration
- Role-based access control (RBAC)
- Account management (profile updates, password changes, deactivation)
- Microservices integration via Eureka discovery

## Features

✅ **Layered Architecture** - Controller → Service → Repository → Security layers
✅ **JWT Authentication** - HS512 signed tokens, 24-hour access, 7-day refresh
✅ **Password Security** - BCrypt hashing with cost factor 12
✅ **Google OAuth2** - Social login integration
✅ **Soft Delete** - Account deactivation preserves historical data
✅ **CORS Configured** - React SPA integration ready
✅ **Comprehensive Logging** - SLF4J/Logback with rotating file appenders
✅ **Global Exception Handling** - Consistent JSON error responses
✅ **Extensive Testing** - Unit tests (Mockito) + integration tests (WebMvc)
✅ **Microservices Ready** - Eureka registration, stateless JWT validation
✅ **Database Indexed** - Optimized queries on email, isActive, provider, currency

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.3.5 |
| Java | OpenJDK | 17 LTS |
| Database | MySQL | 8.0+ |
| Security | Spring Security + JWT (JJWT) | 0.12.6 |
| ORM | Hibernate/JPA | 6.x |
| Data Access | Spring Data JPA | 3.3.5 |
| Discovery | Netflix Eureka | 2023.0.3 |
| Logging | SLF4J/Logback | Latest |
| Testing | JUnit 5 + Mockito | Latest |
| DTOs | Lombok, MapStruct | 1.18.40, 1.5.5 |
| OAuth2 | Spring OAuth2 Client | 6.x |

## Quick Start

### Prerequisites
- Java 17+ (OpenJDK LTS)
- Maven 3.8+
- MySQL 8.0+
- Git

### 1. Clone the Repository
```bash
cd D:\Programs\SprintProjects\SpendSmart\SpendSmart-Backend\auth-service
```

### 2. Create Database
```bash
mysql -u root -p < schema.sql
```

**Or manually:**
```sql
CREATE DATABASE spendsmart_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE spendsmart_auth;
-- Run schema.sql script...
```

### 3. Configure Environment Variables
```bash
# Create .env file or set system variables
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=your_password
export DB_NAME=spendsmart_auth
export JWT_SECRET=your-super-secret-key-minimum-256-bits
export GOOGLE_CLIENT_ID=your-google-oauth2-client-id
export GOOGLE_CLIENT_SECRET=your-google-oauth2-secret
export EUREKA_SERVER_URL=http://localhost:8761/eureka/
export SERVER_PORT=8080
```

### 4. Build the Project
```bash
mvn clean install
```

### 5. Run the Service
```bash
mvn spring-boot:run
```

Or:
```bash
java -jar target/auth-1.0.jar
```

**Service will start on:** `http://localhost:8080`

### 6. Verify Service
```bash
# Check service is running
curl http://localhost:8080/actuator/health

# Check Eureka registration
curl http://localhost:8761/eureka/apps/auth-service
```

---

## Project Structure

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/spendsmart/auth/
│   │   │   ├── AuthServiceApplication.java       # Bootstrap class
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java           # REST endpoints
│   │   │   │   └── MessageDto.java               # Response DTO
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java              # Interface
│   │   │   │   └── AuthServiceImpl.java           # Implementation
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java           # Data access
│   │   │   ├── entity/
│   │   │   │   └── User.java                     # Domain model
│   │   │   ├── dto/
│   │   │   │   ├── RegisterDto.java
│   │   │   │   ├── LoginDto.java
│   │   │   │   ├── AuthResponseDto.java
│   │   │   │   ├── UserProfileDto.java
│   │   │   │   ├── ProfileUpdateDto.java
│   │   │   │   ├── PasswordChangeDto.java
│   │   │   │   ├── RefreshTokenDto.java
│   │   │   │   └── CurrencyUpdateDto.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java   # Error handling
│   │   │   │   ├── ErrorResponseDto.java
│   │   │   │   ├── UserAlreadyExistsException.java
│   │   │   │   ├── InvalidCredentialsException.java
│   │   │   │   ├── TokenRefreshException.java
│   │   │   │   ├── DeactivatedAccountException.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── OAuth2AuthenticationException.java
│   │   │   └── security/
│   │   │       ├── SecurityConfig.java           # Security configuration
│   │   │       ├── JwtUtils.java                 # Token utilities
│   │   │       ├── JwtAuthenticationFilter.java  # Request filter
│   │   │       ├── JwtUserDetails.java           # User context
│   │   │       ├── JwtAuthenticationEntryPoint.java
│   │   │       └── JwtAccessDeniedHandler.java
│   │   └── resources/
│   │       ├── application.yml                   # Main config
│   │       └── logback-spring.xml                # Logging config
│   └── test/
│       └── java/com/spendsmart/auth/
│           ├── AuthServiceApplicationTests.java
│           ├── service/AuthServiceImplTest.java  # 40+ unit tests
│           └── controller/AuthControllerTest.java # 15+ integration tests
├── pom.xml                                        # Maven dependencies
├── schema.sql                                     # Database schema
├── 1-architecture-flow.md                         # Architecture docs
├── 2-inter-service-guide.md                       # Backend integration
├── 3-frontend-integration.md                      # React integration
└── README.md                                      # This file
```

---

## API Endpoints

### Public Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/auth/register` | User registration |
| POST | `/auth/login` | User login |
| POST | `/auth/refresh` | Refresh access token |

### Protected Endpoints (Require JWT)

| Method | Endpoint | Purpose | Auth Required |
|--------|----------|---------|---|
| GET | `/auth/profile` | Get user profile | ✅ |
| PUT | `/auth/profile` | Update profile | ✅ |
| PUT | `/auth/password` | Change password | ✅ |
| PUT | `/auth/currency` | Update currency | ✅ |
| PUT | `/auth/deactivate` | Deactivate account | ✅ |
| POST | `/auth/logout` | Logout user | ✅ |

### Example Requests

**Register:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "John Doe",
    "email": "john@example.com",
    "password": "securePass123",
    "passwordConfirm": "securePass123"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "securePass123"
  }'
```

**Get Profile (Protected):**
```bash
curl -X GET http://localhost:8080/auth/profile \
  -H "Authorization: Bearer <access_token>"
```

---

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=AuthServiceImplTest
mvn test -Dtest=AuthControllerTest
```

### Test Coverage
```bash
mvn jacoco:report
```

### Test Statistics
- **Service Layer Tests:** 40+ test cases
  - Registration (success, duplicate, validation)
  - Login (success, invalid email, invalid password, deactivated)
  - OAuth2 (new user, returning user)
  - Password changes
  - Token validation
  - Account operations

- **Controller Layer Tests:** 15+ integration tests
  - HTTP status codes (200, 201, 400, 401, 403, 409)
  - Request/response validation
  - Exception translation to JSON

---

## Database Schema

### Users Table
| Column | Type | Constraints | Notes |
|--------|------|-----------|-------|
| user_id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| full_name | VARCHAR(255) | NOT NULL | User's name |
| email | VARCHAR(255) | UNIQUE, INDEXED | Login identifier |
| password_hash | VARCHAR(255) | NULLABLE | NULL for OAuth2 users |
| currency | VARCHAR(10) | DEFAULT 'USD' | ISO 4217 code |
| timezone | VARCHAR(50) | DEFAULT 'UTC' | IANA timezone |
| avatar_url | VARCHAR(2048) | NULLABLE | Profile picture |
| provider | ENUM | DEFAULT 'LOCAL' | LOCAL or GOOGLE |
| role | ENUM | DEFAULT 'USER' | USER or ADMIN |
| is_active | BOOLEAN | DEFAULT true | Soft delete flag |
| created_at | TIMESTAMP | NOT NULL | Audit trail |
| updated_at | TIMESTAMP | NOT NULL | Audit trail |
| monthly_budget | DOUBLE | DEFAULT 5000.0 | Financial data |

**Indexes:**
- `email` (UNIQUE)
- `is_active`
- `provider`
- `currency`
- `created_at`

---

## Security Considerations

### Password Security
- ✅ BCrypt hashing with cost factor 12
- ✅ Never stored in plain text
- ✅ Never logged or exposed in APIs
- ✅ Validated against hash using constant-time comparison

### JWT Security
- ✅ HS512 algorithm with 256+ bit secret
- ✅ Secret loaded from environment (never hardcoded)
- ✅ 24-hour access token expiration
- ✅ 7-day refresh token expiration
- ✅ Signature validation on every request
- ✅ Claims include userId and role for authorization

### Network Security
- ✅ HTTPS required in production
- ✅ CORS configured for frontend origins
- ✅ CSRF disabled (stateless JWT)
- ✅ Secure password encoding
- ✅ No sensitive data in logs

### Data Protection
- ✅ Soft delete (isActive flag) preserves data
- ✅ Audit timestamps (createdAt, updatedAt)
- ✅ No hard deletes of user data
- ✅ Email indexed for security checks
- ✅ Account deactivation prevents login

---

## Configuration

### application.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spendsmart_auth
    username: root
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 hours
  refresh-expiration: 604800000  # 7 days

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Environment Variables (Required)
```bash
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=password
DB_NAME=spendsmart_auth
JWT_SECRET=your-256-bit-secret-key
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
EUREKA_SERVER_URL=http://localhost:8761/eureka/
```

---

## Logging

### Log Levels
- **DEBUG:** Detailed operations (token creation, user lookups)
- **INFO:** Important events (registration, login, OAuth2)
- **WARN:** Failed attempts (invalid credentials, duplicate email)
- **ERROR:** Exceptions and system failures

### Log Files
- **Location:** `logs/auth-service.log`
- **Rotation:** Daily + 10MB size limit
- **Retention:** 30 days
- **Error Log:** `logs/auth-service-error.log` (ERROR level only)

---

## Troubleshooting

### Database Connection Error
```
java.sql.SQLException: Cannot get a connection, pool error
```
**Solution:** Check DB_HOST, DB_PORT, DB_USER, DB_PASSWORD environment variables

### JWT Token Invalid
```
Invalid signature error
```
**Solution:** Verify JWT_SECRET is identical across all services

### CORS Error in Frontend
```
No 'Access-Control-Allow-Origin' header
```
**Solution:** Add your frontend URL to `cors.setAllowedOrigins()` in SecurityConfig

### Eureka Registration Failing
```
Failed to register with Eureka
```
**Solution:** Check EUREKA_SERVER_URL and network connectivity to Eureka server

---

## Performance Tuning

### Database Connection Pool
- **Max Pool Size:** 10
- **Min Idle:** 5
- **Connection Timeout:** 20 seconds

### JWT Validation
- **Cached:** No (stateless validation on each request)
- **Performance:** ~1-2ms per request

### Token Generation
- **Algorithm:** HS512 (fast)
- **Performance:** ~5-10ms per token

---

## Deployment

### Docker (Recommended)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/auth-1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build Image
```bash
docker build -t spendsmart-auth:1.0 .
```

### Run Container
```bash
docker run -p 8080:8080 \
  -e DB_HOST=mysql \
  -e JWT_SECRET=your-secret \
  spendsmart-auth:1.0
```

### Kubernetes (Production)
See `k8s/` directory for deployment manifests

---

## Documentation

### Internal Documentation
1. **1-architecture-flow.md** - Service architecture and data flows
2. **2-inter-service-guide.md** - Backend microservices integration
3. **3-frontend-integration.md** - React SPA integration guide

### External Documentation
- [Spring Security Docs](https://spring.io/projects/spring-security)
- [JJWT Docs](https://github.com/jwtk/jjwt)
- [Spring Data JPA Docs](https://spring.io/projects/spring-data-jpa)

---

## Contributing

1. Follow the layered architecture pattern
2. Add comprehensive logging (DEBUG, INFO, WARN, ERROR)
3. Write unit tests (Mockito) + integration tests (WebMvc)
4. Update documentation when adding features
5. Use meaningful commit messages

---

## License

© 2026 SpendSmart. All rights reserved.

---

## Support

For issues, questions, or suggestions:
1. Check the documentation files
2. Review test cases for examples
3. Check logs for detailed error information
4. Open an issue in the repository

---

## Version History

### v1.0 (2026-04-15)
- ✅ Initial release
- ✅ User registration and login
- ✅ JWT authentication
- ✅ Google OAuth2 integration
- ✅ Profile management
- ✅ Password change
- ✅ Account deactivation
- ✅ Comprehensive testing
- ✅ Production-ready code quality

