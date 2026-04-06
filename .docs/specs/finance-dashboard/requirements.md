# Requirements Document

## Introduction

A backend system for a Finance Data Processing and Access Control Dashboard built with Java + Spring Boot. The system supports user management with role-based access control, financial records CRUD with filtering, aggregated dashboard analytics, and clean validation and error handling. The architecture follows clean layered principles with strict separation of concerns.

## Glossary

- **System**: The Finance Dashboard Backend application
- **User**: A registered account with an assigned role
- **Role**: An enumerated permission level (VIEWER, ANALYST, ADMIN)
- **FinancialRecord**: A single financial transaction entry (income or expense)
- **JWT**: JSON Web Token used for stateless authentication
- **Dashboard**: Aggregated analytics computed from financial records
- **AuthService**: The component responsible for registration and login
- **UserService**: The component responsible for user management operations
- **RecordService**: The component responsible for financial record operations
- **DashboardService**: The component responsible for computing aggregated analytics
- **JwtFilter**: The security filter that validates JWT tokens on incoming requests
- **GlobalExceptionHandler**: The component that intercepts and formats all application exceptions

---

## Requirements

### Requirement 1: User Registration and Authentication

**User Story:** As a new user, I want to register and log in, so that I can access the system with a secure token.

#### Acceptance Criteria

1. WHEN a POST request is made to `/auth/register` with a valid name, unique email, and password, THE AuthService SHALL create a new User with role VIEWER, status ACTIVE, and a bcrypt-hashed password.
2. WHEN a POST request is made to `/auth/register` with an email that already exists, THE AuthService SHALL return HTTP 400 with a descriptive error message.
3. WHEN a POST request is made to `/auth/login` with valid credentials, THE AuthService SHALL return a signed JWT token containing the user's id, email, and role.
4. WHEN a POST request is made to `/auth/login` with invalid credentials, THE AuthService SHALL return HTTP 401 with an error message.
5. IF the registration request body is missing required fields, THEN THE System SHALL return HTTP 400 with field-level validation errors.

---

### Requirement 2: Role-Based Access Control

**User Story:** As a system administrator, I want strict role-based access control enforced on every endpoint, so that users can only perform actions permitted by their role.

#### Acceptance Criteria

1. WHILE a request carries a JWT token for a VIEWER role, THE System SHALL permit access only to dashboard summary endpoints.
2. WHILE a request carries a JWT token for an ANALYST role, THE System SHALL permit access to financial record read endpoints and dashboard endpoints.
3. WHILE a request carries a JWT token for an ADMIN role, THE System SHALL permit access to all endpoints including user management and financial record write operations.
4. WHEN a request is made to a protected endpoint without a valid JWT token, THE System SHALL return HTTP 401.
5. WHEN a request is made to an endpoint that the authenticated user's role does not permit, THE System SHALL return HTTP 403.
6. THE JwtFilter SHALL validate the JWT signature and expiry on every protected request before passing it to the controller.

---

### Requirement 3: User Management

**User Story:** As an admin, I want to manage users, so that I can control who has access and at what permission level.

#### Acceptance Criteria

1. WHEN an ADMIN makes a GET request to `/users`, THE UserService SHALL return a paginated list of all users.
2. WHEN an ADMIN makes a PUT request to `/users/{id}/role` with a valid role value, THE UserService SHALL update the target user's role.
3. WHEN an ADMIN makes a PATCH request to `/users/{id}/status` with a valid status value, THE UserService SHALL update the target user's status to ACTIVE or INACTIVE.
4. IF a request is made to `/users/{id}/role` or `/users/{id}/status` with a non-existent user id, THEN THE UserService SHALL return HTTP 404.
5. WHEN a non-ADMIN user attempts to access any `/users` endpoint, THE System SHALL return HTTP 403.
6. WHILE a User's status is INACTIVE, THE System SHALL reject login attempts for that user with HTTP 401.

---

### Requirement 4: Financial Records CRUD

**User Story:** As an analyst or admin, I want to create, read, update, and delete financial records, so that the system maintains an accurate ledger of transactions.

#### Acceptance Criteria

1. WHEN an ADMIN makes a POST request to `/records` with valid amount, type, category, date, and description, THE RecordService SHALL persist a new FinancialRecord linked to the authenticated user as `createdBy`.
2. WHEN an ANALYST or ADMIN makes a GET request to `/records`, THE RecordService SHALL return a paginated list of financial records.
3. WHEN an ADMIN makes a PUT request to `/records/{id}` with valid fields, THE RecordService SHALL update the specified record.
4. WHEN an ADMIN makes a DELETE request to `/records/{id}`, THE RecordService SHALL soft-delete the record by marking it inactive rather than removing it from the database.
5. IF a request references a non-existent record id, THEN THE RecordService SHALL return HTTP 404.
6. IF the record request body fails validation (e.g., negative amount, missing type), THEN THE System SHALL return HTTP 400 with field-level errors.

---

### Requirement 5: Financial Record Filtering

**User Story:** As an analyst, I want to filter financial records by date range, category, and type, so that I can analyze specific subsets of data.

#### Acceptance Criteria

1. WHEN a GET request to `/records` includes `startDate` and `endDate` query parameters, THE RecordService SHALL return only records whose date falls within that range (inclusive).
2. WHEN a GET request to `/records` includes a `category` query parameter, THE RecordService SHALL return only records matching that category.
3. WHEN a GET request to `/records` includes a `type` query parameter of INCOME or EXPENSE, THE RecordService SHALL return only records of that type.
4. WHEN multiple filter parameters are combined, THE RecordService SHALL apply all filters conjunctively (AND logic).
5. WHEN no filter parameters are provided, THE RecordService SHALL return all non-deleted records with pagination.

---

### Requirement 6: Dashboard Analytics

**User Story:** As a viewer, analyst, or admin, I want to see aggregated financial summaries, so that I can understand the overall financial state.

#### Acceptance Criteria

1. WHEN a GET request is made to `/dashboard/summary`, THE DashboardService SHALL compute and return total income, total expenses, and net balance from non-deleted records.
2. WHEN a GET request is made to `/dashboard/summary`, THE DashboardService SHALL also return category-wise totals grouped by category name.
3. WHEN a GET request is made to `/dashboard/trends`, THE DashboardService SHALL return monthly aggregated income and expense totals for the current year.
4. WHEN a GET request is made to `/dashboard/summary`, THE DashboardService SHALL include the 5 most recent non-deleted transactions ordered by date descending.
5. THE DashboardService SHALL compute all aggregations via service/query logic and SHALL NOT return hardcoded values.

---

### Requirement 7: Validation and Error Handling

**User Story:** As an API consumer, I want consistent and descriptive error responses, so that I can handle failures gracefully in client applications.

#### Acceptance Criteria

1. THE GlobalExceptionHandler SHALL intercept all unhandled exceptions and return a structured JSON error response containing a timestamp, HTTP status code, and message.
2. WHEN a `@Valid` annotated request body fails validation, THE GlobalExceptionHandler SHALL return HTTP 400 with a map of field names to error messages.
3. WHEN a resource is not found, THE GlobalExceptionHandler SHALL return HTTP 404 with a descriptive message.
4. WHEN authentication fails, THE GlobalExceptionHandler SHALL return HTTP 401.
5. WHEN authorization fails, THE GlobalExceptionHandler SHALL return HTTP 403.
6. THE System SHALL never expose internal stack traces or sensitive system information in error responses.

---

### Requirement 8: Database Design and Integrity

**User Story:** As a developer, I want a normalized and indexed database schema, so that queries are efficient and data integrity is maintained.

#### Acceptance Criteria

1. THE System SHALL define a `users` table with columns: id, name, email (unique, indexed), password, role, status, created_at.
2. THE System SHALL define a `financial_records` table with columns: id, amount, type, category, date (indexed), description, created_by (FK to users), deleted (soft-delete flag), created_at.
3. THE System SHALL enforce a foreign key relationship between `financial_records.created_by` and `users.id`.
4. WHEN a FinancialRecord is "deleted", THE System SHALL set the `deleted` flag to true and SHALL NOT remove the row from the database.
5. THE System SHALL use JPA/Hibernate to manage schema creation and relationships.

---

### Requirement 9: API Documentation and Project Setup

**User Story:** As a developer onboarding to this project, I want clear setup instructions and API documentation, so that I can run and consume the system quickly.

#### Acceptance Criteria

1. THE System SHALL include a `README.md` with setup steps, environment configuration, and API endpoint reference.
2. WHERE Swagger is enabled, THE System SHALL expose an OpenAPI UI at `/swagger-ui.html` listing all endpoints with request/response schemas.
3. THE System SHALL be structured as a Maven project under the base package `com.project.finance`.
4. THE System SHALL include an `application.properties` (or `application.yml`) with configurable database URL, JWT secret, and token expiry.
