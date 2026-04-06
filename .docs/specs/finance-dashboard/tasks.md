# Implementation Plan: Finance Dashboard Backend

## Overview

Incremental implementation of the Finance Dashboard backend in Java + Spring Boot. Each task builds on the previous, ending with a fully wired system. Tasks reference specific requirements and design properties for traceability.

---

## Tasks

- [x] 1. Initialize Maven project and core infrastructure
  - Create Maven project under `com.project.finance` with Spring Boot parent POM
  - Add dependencies: spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-data-jpa, spring-boot-starter-validation, jjwt (JWT), mysql-connector-j (or postgresql), springdoc-openapi-ui, jqwik (test), h2 (test)
  - Create `application.properties` with configurable `spring.datasource.*`, `app.jwt.secret`, `app.jwt.expiration-ms`
  - Create the full package skeleton: controller, service, repository, model, dto/request, dto/response, security, exception, config
  - _Requirements: 9.3, 9.4_

- [x] 2. Implement domain model and enums
  - [x] 2.1 Create enums: `Role` (VIEWER, ANALYST, ADMIN), `UserStatus` (ACTIVE, INACTIVE), `RecordType` (INCOME, EXPENSE)
    - _Requirements: 1.1, 4.1_
  - [x] 2.2 Create `User` entity with JPA annotations, unique index on email, `@PrePersist` for `createdAt`
    - _Requirements: 8.1_
  - [x] 2.3 Create `FinancialRecord` entity with `@ManyToOne` to User, index on date, `deleted` flag defaulting to false, `@PrePersist` for `createdAt`
    - _Requirements: 8.2, 8.3, 8.4_

- [x] 3. Implement repositories
  - [x] 3.1 Create `UserRepository` extending `JpaRepository<User, Long>` with `findByEmail(String email)` and `existsByEmail(String email)`
    - _Requirements: 1.1, 1.2_
  - [x] 3.2 Create `FinancialRecordRepository` with:
    - `findAllByDeletedFalse(Pageable)` for paginated listing
    - A `@Query` method accepting optional date range, category, and type filters (use Specification or JPQL with dynamic predicates)
    - `@Query` for total income/expense sums (WHERE deleted=false)
    - `@Query` for category-wise totals (GROUP BY category WHERE deleted=false)
    - `@Query` for monthly trends (GROUP BY YEAR(date), MONTH(date), type WHERE deleted=false AND YEAR(date)=:year)
    - `findTop5ByDeletedFalseOrderByDateDesc()` for recent transactions
    - _Requirements: 5.1–5.4, 6.1–6.4, 8.2_

- [x] 4. Implement DTOs
  - [x] 4.1 Create request DTOs: `RegisterRequest` (@NotBlank name, @Email email, @Size(min=6) password), `LoginRequest`, `UpdateRoleRequest`, `UpdateStatusRequest`, `RecordRequest` (@Positive amount, @NotNull type/date/category)
    - _Requirements: 1.5, 4.6_
  - [x] 4.2 Create response DTOs: `AuthResponse` (token, email, role), `UserResponse`, `RecordResponse`, `DashboardSummaryResponse` (totalIncome, totalExpenses, netBalance, categoryTotals, recentTransactions), `TrendResponse` (year, month, totalIncome, totalExpenses)
    - _Requirements: 1.3, 6.1–6.4_

- [x] 5. Implement JWT security infrastructure
  - [x] 5.1 Create `JwtUtil` with `generateToken(UserDetails)`, `extractEmail(token)`, `isTokenValid(token, UserDetails)` using HMAC-SHA256 and configurable secret/expiry
    - _Requirements: 1.3, 2.6_
  - [x] 5.2 Write property test for JwtUtil — Property 2 and Property 4

    - **Property 2: JWT round-trip preserves identity**
    - **Property 4: Invalid JWT tokens are always rejected**
    - **Validates: Requirements 1.3, 2.4, 2.6**
  - [x] 5.3 Create `UserDetailsServiceImpl` implementing `UserDetailsService`, loading user by email, throwing `InactiveUserException` if status=INACTIVE
    - _Requirements: 3.6_
  - [x] 5.4 Create `JwtAuthenticationFilter` extending `OncePerRequestFilter`: extract Bearer token, validate via JwtUtil, set `SecurityContextHolder` authentication; return 401 on invalid token
    - _Requirements: 2.4, 2.6_
  - [x] 5.5 Create `SecurityConfig` (`@Configuration @EnableWebSecurity @EnableMethodSecurity`): configure filter chain with public routes (/auth/**, /swagger-ui/**, /v3/api-docs/**), add JwtAuthenticationFilter, disable CSRF, set stateless session
    - _Requirements: 2.1–2.5_

- [x] 6. Implement exception classes and global handler
  - [x] 6.1 Create custom exceptions: `ResourceNotFoundException`, `DuplicateEmailException`, `InactiveUserException`
    - _Requirements: 7.3_
  - [x] 6.2 Create `GlobalExceptionHandler` (`@RestControllerAdvice`) handling: `MethodArgumentNotValidException` → 400 with field errors map, `DuplicateEmailException` → 400, `ResourceNotFoundException` → 404, `InactiveUserException` → 401, `AuthenticationException` → 401, `AccessDeniedException` → 403, generic `Exception` → 500; all responses include timestamp, status, message; no stack traces exposed
    - _Requirements: 7.1–7.6_
  - [x] 6.3 Write property test for GlobalExceptionHandler — Property 12

    - **Property 12: Error responses are structured and safe**
    - **Validates: Requirements 7.1, 7.4, 7.6**

- [x] 7. Implement AuthService and AuthController
  - [x] 7.1 Implement `AuthService`:
    - `register(RegisterRequest)`: check duplicate email (throw `DuplicateEmailException`), hash password with BCrypt, save User with role=VIEWER and status=ACTIVE, return `AuthResponse`
    - `login(LoginRequest)`: authenticate via `AuthenticationManager`, check status=ACTIVE, generate JWT, return `AuthResponse`
    - _Requirements: 1.1–1.4_
  - [x] 7.2 Write property test for AuthService — Property 1 and Property 5

    - **Property 1: Registration defaults are always correct**
    - **Property 5: Inactive users cannot authenticate**
    - **Validates: Requirements 1.1, 3.6**
  - [x] 7.3 Implement `AuthController` with `POST /auth/register` and `POST /auth/login`, both using `@Valid` on request body
    - _Requirements: 1.1–1.5_

- [x] 8. Checkpoint — auth and security baseline
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement UserService and UserController
  - [x] 9.1 Implement `UserService`:
    - `getAllUsers(Pageable)`: return paginated `UserResponse` list
    - `updateRole(Long id, Role role)`: find user (throw `ResourceNotFoundException` if missing), update role, save, return `UserResponse`
    - `updateStatus(Long id, UserStatus status)`: find user, update status, save, return `UserResponse`
    - _Requirements: 3.1–3.4_
  - [x] 9.2 Write property test for UserService — Property 6

    - **Property 6: Role update round-trip**
    - **Validates: Requirements 3.2**
  - [x] 9.3 Implement `UserController` with `@PreAuthorize("hasRole('ADMIN')")` on all methods:
    - `GET /users` with `Pageable` parameter
    - `PUT /users/{id}/role`
    - `PATCH /users/{id}/status`
    - _Requirements: 3.1–3.5_

- [x] 10. Implement RecordService and RecordController
  - [x] 10.1 Implement `RecordService`:
    - `createRecord(RecordRequest, User)`: persist new `FinancialRecord` with `createdBy` set to authenticated user
    - `getRecords(Pageable, LocalDate startDate, LocalDate endDate, String category, RecordType type)`: delegate to repository with dynamic filtering
    - `updateRecord(Long id, RecordRequest)`: find non-deleted record (throw `ResourceNotFoundException`), update fields, save
    - `deleteRecord(Long id)`: find non-deleted record, set `deleted=true`, save (soft delete)
    - _Requirements: 4.1–4.5, 5.1–5.4_
  - [x] 10.2 Write property test for RecordService — Property 7, Property 8, Property 9

    - **Property 7: Record create round-trip**
    - **Property 8: Soft delete excludes records from queries**
    - **Property 9: Combined filter correctness**
    - **Validates: Requirements 4.1, 4.4, 5.1–5.4, 8.4**
  - [x] 10.3 Implement `RecordController`:
    - `POST /records` — `@PreAuthorize("hasRole('ADMIN')")`
    - `GET /records` — `@PreAuthorize("hasAnyRole('ANALYST','ADMIN')")` with optional query params: startDate, endDate, category, type; plus `Pageable`
    - `PUT /records/{id}` — `@PreAuthorize("hasRole('ADMIN')")`
    - `DELETE /records/{id}` — `@PreAuthorize("hasRole('ADMIN')")`, returns 204
    - _Requirements: 4.1–4.6, 5.1–5.4_

- [x] 11. Implement DashboardService and DashboardController
  - [x] 11.1 Implement `DashboardService`:
    - `getSummary()`: query totalIncome, totalExpenses, compute netBalance, query categoryTotals, query top-5 recent transactions; assemble into `DashboardSummaryResponse`
    - `getTrends()`: query monthly aggregations for current year; assemble into `List<TrendResponse>`
    - All values computed from repository queries, none hardcoded
    - _Requirements: 6.1–6.5_
  - [x] 11.2 Write property test for DashboardService — Property 10 and Property 11

    - **Property 10: Dashboard summary invariant**
    - **Property 11: Monthly trends correctness**
    - **Validates: Requirements 6.1–6.4**
  - [x] 11.3 Implement `DashboardController`:
    - `GET /dashboard/summary` — `@PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")`
    - `GET /dashboard/trends` — `@PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")`
    - _Requirements: 6.1–6.5_

- [x] 12. Checkpoint — core feature complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Implement RBAC property test
  - [x] 13.1 Write property test for RBAC enforcement — Property 3

    - **Property 3: RBAC enforcement is consistent**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.5**
  - _Note: Uses MockMvc with generated role/endpoint combinations to verify 200 vs 403 responses_

- [x] 14. Add Swagger/OpenAPI configuration and README
  - [x] 14.1 Create `SwaggerConfig` with `@OpenAPIDefinition` (title, version, description) and `@SecurityScheme` for Bearer JWT; annotate controllers with `@Tag` and `@Operation`
    - _Requirements: 9.1, 9.2_
  - [x] 14.2 Create `README.md` with: prerequisites, setup steps (clone, configure DB, run), environment variables reference, full API endpoint table with roles, sample curl requests, assumptions
    - _Requirements: 9.1_

- [x] 15. Final checkpoint — full system wired
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik with H2 in-memory DB; minimum 100 tries per property
- Soft delete is used throughout — hard deletes are never performed
- All aggregation logic lives in the service layer; controllers only delegate
