# SpendSmart Project Report

## 1) Executive Summary
SpendSmart is a full-stack, microservices-based personal finance platform with a React frontend and a Spring Boot backend ecosystem. The system is designed for individual users to manage expenses, income, categories, budgets, recurring rules, analytics, and notifications, while also supporting an admin control plane for platform oversight.

The repository currently consists of:
- `SpendSmart-Frontend/spendsmart` (React SPA)
- `SpendSmart-Backend` (10 Spring services)

Core capabilities implemented in this project include:
- JWT-based authentication with refresh tokens
- OTP verification workflow (signup/login 2FA)
- Google OAuth2 login
- User profile and preference management
- Full CRUD for expenses, incomes, categories, budgets, recurring rules
- Budget spent synchronization from expense events
- Analytics snapshots, trends, CSV/PDF export endpoints
- In-app notifications and admin broadcast messaging
- Service discovery + API gateway routing across microservices

---

## 2) High-Level Architecture
### 2.1 Logical Architecture
- Frontend SPA talks to a single API entrypoint (`web-service` on port `8080`)
- API Gateway routes requests to domain microservices via Eureka service discovery
- Services persist to per-service MySQL schemas (database-per-service model)
- Auth service publishes events to RabbitMQ
- Notification service consumes those events and delivers in-app + email notifications
- Redis is used in auth for OTP and token blocklist behavior (with in-memory fallback for revocation)

### 2.2 Backend Service Inventory
- `discovery-server` (Eureka registry) - port `8761`
- `web-service` (Spring Cloud Gateway) - port `8080`
- `auth-service` - port `8081`
- `expense-service` - port `8082`
- `budget-service` - port `8083`
- `category-service` - port `8084`
- `income-service` - port `8085`
- `analytics-service` - port `8086`
- `notification-service` - port `8087`
- `recurring-service` - port `8088`

### 2.3 Gateway Route Map
The gateway routes by path prefix:
- `/auth/**`, `/oauth2/**`, `/login/oauth2/**` -> `AUTH-SERVICE`
- `/expenses/**` -> `EXPENSE-SERVICE`
- `/incomes/**` -> `INCOME-SERVICE`
- `/categories/**` -> `CATEGORY-SERVICE`
- `/budgets/**` -> `BUDGET-SERVICE`
- `/analytics/**` -> `ANALYTICS-SERVICE`
- `/recurring/**` -> `RECURRING-SERVICE`
- `/notifications/**` -> `NOTIFICATION-SERVICE`
- Admin-prefixed routes are split across auth/expense/income/category/notification services

---

## 3) Technology Stack and Why It Was Chosen

## 3.1 Backend Stack
- Java 17 (LTS)
- Spring Boot 3.3.5
- Spring Cloud 2023.0.3
- Spring Security + custom JWT filters
- Spring Data JPA + Hibernate
- MySQL
- Redis
- RabbitMQ
- Springdoc OpenAPI (Swagger)
- Lombok
- MapStruct (select modules)
- Maven

Why this stack over alternatives:
- Spring Boot over lower-level frameworks:
  - Faster delivery with strong conventions, mature ecosystem, and operational defaults.
- Microservices over single monolith:
  - Clear bounded contexts (`auth`, `expense`, `budget`, etc.), independent scaling/deployment, and cleaner ownership boundaries.
- Eureka discovery over static service URLs:
  - Dynamic registration and service instance lookup simplify routing and horizontal scaling.
- API Gateway over direct frontend-to-service calls:
  - Centralized CORS, routing, security headers, and future policy controls.
- MySQL + JPA over NoSQL for this domain:
  - Financial records favor relational integrity, consistency, and transactional guarantees.
- JWT stateless auth over session-based auth:
  - Better fit for distributed services and gateway-centric API communication.
- Redis for OTP/blocklist over pure DB polling:
  - TTL-driven ephemeral state is faster and operationally simpler for short-lived tokens/challenges.
- RabbitMQ for auth notification events over synchronous REST fan-out:
  - Better decoupling and resilience; auth responses are not blocked by downstream notification work.

## 3.2 Frontend Stack
- React 18 + Vite 5
- React Router v6
- Axios with auth/refresh interceptors
- TanStack Query (installed; selective adoption)
- Tailwind CSS + Radix UI primitives + utility components
- Framer Motion
- Recharts
- Vitest + Testing Library

Why this stack over alternatives:
- React + Vite over heavier SPA tooling:
  - Fast local startup/build with modern ESM workflow.
- Router-based SPA over MPA:
  - Better authenticated app UX and guarded routes.
- Axios interceptors over manual token handling in each API call:
  - Centralized retry/refresh logic and consistent auth headers.
- Tailwind + component primitives over custom CSS-only UI from scratch:
  - Faster, consistent UI composition with flexible theming.
- Recharts + Framer Motion:
  - Good developer velocity for dashboard visuals and interaction polish.

---

## 4) Backend Module-by-Module Report

## 4.1 `auth-service`
Primary responsibilities:
- Register/login/refresh/logout
- Google OAuth2 login callback integration
- OTP challenge generation + verification (signup and 2FA)
- Profile management, password change, currency update, account deactivation
- Admin user management (status/role/subscription updates)

Notable implementation details:
- User model supports `LOCAL` and `GOOGLE` providers
- Soft-deactivation with `isActive`
- Refresh token rotation behavior with token revocation
- OTP challenge TTL handled via Redis
- Auth events published to RabbitMQ (`spendsmart.auth.events`)

Key endpoint groups:
- `/auth/*`
- `/admin/users/*`

## 4.2 `expense-service`
Primary responsibilities:
- Expense CRUD with filters (keyword/date/category/month/type/payment)
- Totals calculation
- Receipt upload endpoint
- Admin read-only expense aggregation endpoints

Notable implementation details:
- Strong user scoping (`userId` from JWT context)
- Synchronous spent-delta sync to budget service via `RestTemplate`
- Receipt storage implemented on local filesystem (`./receipts/...`)

Key endpoint groups:
- `/expenses/*`
- `/admin/expenses/*`

## 4.3 `budget-service`
Primary responsibilities:
- Budget CRUD
- Active/exceeded budget retrieval
- Budget spent updates

Notable implementation details:
- Internal spent update endpoint `/budgets/spent` is intentionally exposed (`permitAll`) for service-to-service use
- Utility methods for threshold and over-budget detection in entity

Key endpoint groups:
- `/budgets/*`

## 4.4 `category-service`
Primary responsibilities:
- Category CRUD
- Type-based category retrieval
- Default category seeding
- Budget limit per category
- Category validation endpoint
- Admin category listing/search/count

Notable implementation details:
- Free-tier category cap logic (`NORMAL` plan custom-category limit per type)
- Default category template seeding for new users

Key endpoint groups:
- `/categories/*`
- `/admin/categories/*`

## 4.5 `income-service`
Primary responsibilities:
- Income CRUD
- Filter by source/date/keyword
- Totals by source and overall
- Admin listing/count

Notable implementation details:
- Recurrence-related fields included on income model
- Source enum mapping and totals response map

Key endpoint groups:
- `/incomes/*`
- `/admin/incomes/*`

## 4.6 `recurring-service`
Primary responsibilities:
- Recurring rule CRUD
- Active toggling
- Due-rule queries
- Active rule counts

Notable implementation details:
- Rule carries next due date and frequency enum
- Entity includes next-due-date calculation helper

Key endpoint groups:
- `/recurring/*`

## 4.7 `analytics-service`
Primary responsibilities:
- Latest summary
- Date-range summary series
- Monthly trends
- Snapshot history
- CSV/PDF export endpoints for paid users

Notable implementation details:
- Snapshot-based persistence avoids on-demand heavy aggregation
- CSV export implemented; PDF export currently returns placeholder bytes (`"PDF Data"`)

Key endpoint groups:
- `/analytics/*`

## 4.8 `notification-service`
Primary responsibilities:
- User notification inbox (list/unread/count/read/delete)
- Internal create endpoint for inter-service notifications
- RabbitMQ listener for auth events
- Admin broadcasts to non-admin users
- Email notifications (OTP + paid-user broadcasts)

Notable implementation details:
- `@RabbitListener` consumes auth events and stores in-app notifications
- Admin broadcast fetches users from `AUTH-SERVICE` through load-balanced `RestTemplate`
- `/notifications/internal` is intentionally open for inter-service calls

Key endpoint groups:
- `/notifications/*`
- `/admin/notifications/*`

## 4.9 `web-service` (API Gateway)
Primary responsibilities:
- Single ingress for frontend
- Route delegation to downstream services
- CORS and security response headers

## 4.10 `discovery-server`
Primary responsibilities:
- Eureka service registration and lookup for all microservices

---

## 5) Frontend Report

## 5.1 Application Structure
Core areas in `src/`:
- `pages/` - user + admin screens
- `components/` - layout, guards, chart blocks, UI building blocks
- `contexts/` - auth and category shared state
- `lib/services/` - API wrappers per backend domain
- `lib/` - client helpers, formatters, analytics transformations

## 5.2 Auth and Routing
Implemented flows:
- Public routes: landing, login, register, OTP verify, OAuth callback
- Protected user routes: dashboard, expenses, income, categories, budgets, recurring, analytics, notifications, reports, profile
- Admin routes: admin dashboard, users, transactions, analytics, notifications

Security behavior:
- Access and refresh tokens persisted in local storage
- Axios interceptor auto-injects bearer token
- On 401, refresh flow attempts token rotation and retries request
- Route guards enforce auth state and admin role checks

## 5.3 Data Integration
Frontend service clients map cleanly to backend endpoints:
- `authService.js` -> `/auth/*`
- `expenseService.js` -> `/expenses/*`
- `incomeService.js` -> `/incomes/*`
- `categoryService.js` -> `/categories/*`
- `budgetService.js` -> `/budgets/*`
- `recurringService.js` -> `/recurring/*`
- `analyticsService.js` -> `/analytics/*`
- `notificationService.js` -> `/notifications/*`
- Admin service wrappers -> `/admin/*`

## 5.4 UX and Visualization
- Responsive sidebar/dashboard shell
- KPI cards and category/budget visual summaries
- Recharts visualizations
- Animated transitions via Framer Motion

---

## 6) Data and Persistence Model
Pattern used:
- Each service owns its own schema and entities (database-per-service boundary)

Representative aggregates:
- Auth: `users`
- Expense: `expenses`
- Income: `incomes`
- Category: `categories`
- Budget: `budgets`
- Recurring: `recurring_rules`
- Analytics: `analytics_snapshots`
- Notification: `notifications`

Benefits of this approach:
- Service autonomy and independent schema evolution
- Reduced accidental cross-service coupling
- Better alignment with domain boundaries

---

## 7) Security Model
Implemented controls:
- JWT validation filters in each domain service
- Role-gated admin routes (`/admin/**`)
- CORS configuration for local frontend origins
- Standard security headers at gateway
- BCrypt password hashing in auth service
- Token revocation support and refresh-token invalidation pattern

Additional auth hardening already present:
- OTP verification stage for signup and optional login 2FA
- Account `isActive` checks during login/refresh/profile workflows

---

## 8) Async and Inter-Service Communication
Two communication styles are used together:
- Synchronous HTTP:
  - Expense -> Budget spent sync endpoint
  - Admin broadcast flow reading active users from auth service
- Asynchronous events:
  - Auth -> RabbitMQ -> Notification consumer

Why hybrid is appropriate here:
- Synchronous updates are used where immediate consistency is preferred (budget deltas)
- Asynchronous messaging is used where reliability and decoupling are prioritized (notifications/emails)

---

## 9) Build, Run, and Ops Notes

## 9.1 Backend
- Build tool: Maven wrappers available in all services
- Discovery + gateway include Dockerfiles; gateway includes docker-compose
- Swagger endpoints are enabled in services (`/swagger-ui.html`)

## 9.2 Frontend
- Vite dev server on `5173`
- API base configurable via `.env` (`VITE_API_BASE_URL`, defaults to gateway `8080`)
- Build uses manual chunking for key vendor bundles

## 9.3 Infrastructure Dependencies
- MySQL
- Redis
- RabbitMQ
- Eureka discovery server

---

## 10) Testing and Quality Status
Current observed test footprint:
- Backend test files: 18 Java test classes across services
- Frontend test files: baseline Vitest setup with example test

Testing maturity summary:
- Backend has meaningful service/controller test presence across core modules
- Frontend automated testing is currently minimal and can be expanded

Static/quality tooling present:
- Sonar Maven plugin configured across multiple services
- ESLint + Prettier + Vitest configured in frontend

---

## 11) What Has Been Achieved in This Project
Delivered outcomes:
- End-to-end microservices skeleton is operational and integrated
- Domain separation is clear and production-oriented
- Frontend is connected to backend APIs via gateway pattern
- Core personal finance workflows are implemented for users
- Admin workflows are implemented for moderation/oversight
- Security model (JWT + OTP + role access) is in place
- Event-driven notification path is implemented

This is beyond a basic prototype: it is a structured, extensible platform with clear service ownership and practical operational foundations.

---

## 12) Important Observations and Risks (Current State)
These are implementation observations discovered during analysis:
- Sensitive values are currently committed in plain text in multiple configs (examples include OAuth client secret, mail app password, JWT secret defaults, Sonar tokens).
- Some docs/comments are ahead of or behind actual implementation (e.g., references to S3 while receipt storage is local filesystem, some older mock/demo wording in frontend comments).
- `analytics-service` PDF export endpoint is currently a placeholder implementation.
- There are schema/reference SQL artifacts and generated `target/` files in service folders that indicate mixed runtime/reference documentation states.

Recommendation:
- Externalize all secrets immediately to environment/secret manager and rotate exposed credentials.

---

## 13) Why the Current Architecture Is a Good Fit
Given product scope (auth, transactions, budgets, analytics, notifications, admin tooling), the selected architecture is well-justified:
- Microservices provide clean domain boundaries and independent evolution.
- Gateway + discovery simplify frontend integration and service growth.
- Relational persistence suits financial correctness requirements.
- JWT + OTP provides scalable, modern auth controls.
- RabbitMQ decouples user-facing auth latency from notification side effects.
- React SPA gives fast UX iteration for dashboard-heavy interfaces.

---

## 14) Final Assessment
SpendSmart is a comprehensive full-stack finance platform implementation with strong architectural intent and substantial feature coverage already delivered. The project demonstrates:
- Correct use of service decomposition
- Practical API gateway + discovery integration
- Security-first patterns (JWT, role checks, OTP)
- Rich frontend UX with real service integration

Primary immediate improvement area is operational hardening (secret management, consistency cleanup, and deeper frontend test coverage), not core architecture or feature viability.
