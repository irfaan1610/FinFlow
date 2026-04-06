# Design Document: Finance Dashboard Backend

## Overview

The Finance Dashboard Backend is a Spring Boot REST API that provides user management with role-based access control, financial record CRUD, and aggregated analytics. It is structured around clean layered architecture (Controller → Service → Repository → DB) with JWT-based stateless authentication enforced at the filter level.

The system targets three user roles with strictly different permissions:
- **VIEWER** — read-only dashboard access
- **ANALYST** — read financial records + dashboard
- **ADMIN** — full access including writes and user management

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     HTTP Clients                        │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              Spring Security Filter Chain               │
│   JwtAuthenticationFilter → validates token, sets       │
│   SecurityContext before request reaches controller     │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  Controller Layer                       │
│  AuthController | UserController | RecordController     │
│  DashboardController                                    │
│  - Handles HTTP, delegates to service, returns DTOs     │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                   Service Layer                         │
│  AuthService | UserService | RecordService              │
│  DashboardService                                       │
│  - Business logic, validation, aggregation              │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                 Repository Layer                        │
│  UserRepository | FinancialRecordRepository             │
│  - JPA interfaces with custom JPQL queries              │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  Database (MySQL/PostgreSQL)             │
│  users | financial_records                              │
└─────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.project.finance
├── controller
│   ├── AuthController.java
│   ├── UserController.java
│   ├── RecordController.java
│   └── DashboardController.java
├── service
│   ├── AuthService.java
│   ├── UserService.java
│   ├── RecordService.java
│   └── DashboardService.java
├── repository
│   ├── UserRepository.java
│   └── FinancialRecordRepository.java
├── model
│   ├── User.java
│   ├── FinancialRecord.java
│   ├── Role.java          (enum)
│   ├── UserStatus.java    (enum)
│   └── RecordType.java    (enum)
├── dto
│   ├── request
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── UpdateRoleRequest.java
│   │   ├── UpdateStatusRequest.java
│   │   └── RecordRequest.java
│   └── response
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── RecordResponse.java
│       ├── DashboardSummaryResponse.java
│       └── TrendResponse.java
├── security
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── exception
│   ├── ResourceNotFoundException.java
│   ├── DuplicateEmailException.java
│   ├── InactiveUserException.java
│   └── GlobalExceptionHandler.java
└── config
    ├── SecurityConfig.java
    └── SwaggerConfig.java
```

---

## Components and Interfaces

### AuthController
- `POST /auth/register` → `RegisterRequest` → `AuthResponse`
- `POST /auth/login` → `LoginRequest` → `AuthResponse`
- No authentication required on these endpoints

### UserController
- `GET /users` → paginated `UserResponse` list (ADMIN only)
- `PUT /users/{id}/role` → `UpdateRoleRequest` → `UserResponse` (ADMIN only)
- `PATCH /users/{id}/status` → `UpdateStatusRequest` → `UserResponse` (ADMIN only)

### RecordController
- `POST /records` → `RecordRequest` → `RecordResponse` (ADMIN only)
- `GET /records` → paginated `RecordResponse` list with optional filters (ANALYST, ADMIN)
- `PUT /records/{id}` → `RecordRequest` → `RecordResponse` (ADMIN only)
- `DELETE /records/{id}` → 204 No Content (ADMIN only)

### DashboardController
- `GET /dashboard/summary` → `DashboardSummaryResponse` (VIEWER, ANALYST, ADMIN)
- `GET /dashboard/trends` → `List<TrendResponse>` (VIEWER, ANALYST, ADMIN)

### JwtAuthenticationFilter
- Extends `OncePerRequestFilter`
- Extracts Bearer token from `Authorization` header
- Validates signature and expiry via `JwtUtil`
- Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
- Passes 401 if token is invalid or missing on protected routes

### JwtUtil
- `generateToken(UserDetails)` → signed JWT with subject=email, claims: userId, role, expiry
- `extractEmail(token)` → String
- `isTokenValid(token, UserDetails)` → boolean (checks signature + expiry)

### UserDetailsServiceImpl
- Implements Spring's `UserDetailsService`
- Loads user by email from `UserRepository`
- Checks `status == ACTIVE`; throws `InactiveUserException` if INACTIVE

---

## Data Models

### User Entity

```java
@Entity
@Table(name = "users", indexes = @Index(columnList = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;  // bcrypt hash

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;  // VIEWER | ANALYST | ADMIN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;  // ACTIVE | INACTIVE

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### FinancialRecord Entity

```java
@Entity
@Table(name = "financial_records", indexes = @Index(columnList = "date"))
public class FinancialRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType type;  // INCOME | EXPENSE

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private LocalDate date;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### Enums

```java
public enum Role { VIEWER, ANALYST, ADMIN }
public enum UserStatus { ACTIVE, INACTIVE }
public enum RecordType { INCOME, EXPENSE }
```

### Key DTOs

**RegisterRequest**
```java
public class RegisterRequest {
    @NotBlank String name;
    @NotBlank @Email String email;
    @NotBlank @Size(min = 6) String password;
}
```

**RecordRequest**
```java
public class RecordRequest {
    @NotNull @Positive BigDecimal amount;
    @NotNull RecordType type;
    @NotBlank String category;
    @NotNull LocalDate date;
    String description;
}
```

**DashboardSummaryResponse**
```java
public class DashboardSummaryResponse {
    BigDecimal totalIncome;
    BigDecimal totalExpenses;
    BigDecimal netBalance;
    Map<String, BigDecimal> categoryTotals;
    List<RecordResponse> recentTransactions;  // top 5
}
```

**TrendResponse**
```java
public class TrendResponse {
    int year;
    int month;
    BigDecimal totalIncome;
    BigDecimal totalExpenses;
}
```

---

## Security Configuration

```
Public endpoints (no JWT required):
  POST /auth/register
  POST /auth/login
  GET  /swagger-ui/**
  GET  /v3/api-docs/**

Protected endpoints (JWT required):
  /users/**          → ADMIN
  POST /records      → ADMIN
  PUT  /records/**   → ADMIN
  DELETE /records/** → ADMIN
  GET  /records/**   → ANALYST, ADMIN
  GET  /dashboard/** → VIEWER, ANALYST, ADMIN
```

Method-level security is applied via `@PreAuthorize("hasRole('ADMIN')")` annotations as a second layer of defense.

JWT Configuration:
- Algorithm: HMAC-SHA256
- Expiry: configurable via `app.jwt.expiration-ms` (default 86400000 = 24h)
- Secret: configurable via `app.jwt.secret`

---

## Dashboard Aggregation Logic

All aggregations are computed via JPQL queries in `FinancialRecordRepository` and assembled in `DashboardService`. No values are hardcoded.

```
totalIncome  = SUM(amount) WHERE type=INCOME AND deleted=false
totalExpenses = SUM(amount) WHERE type=EXPENSE AND deleted=false
netBalance   = totalIncome - totalExpenses

categoryTotals = GROUP BY category, SUM(amount) WHERE deleted=false

monthlyTrends = GROUP BY YEAR(date), MONTH(date), type
                SUM(amount) WHERE deleted=false AND YEAR(date)=currentYear

recentTransactions = ORDER BY date DESC, LIMIT 5 WHERE deleted=false
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Registration defaults are always correct

*For any* valid registration request (non-blank name, valid email, password ≥ 6 chars), the created User entity SHALL have role=VIEWER, status=ACTIVE, and a password that is a valid bcrypt hash (not equal to the plaintext password).

**Validates: Requirements 1.1**

---

### Property 2: JWT round-trip preserves identity

*For any* registered and active user, logging in with correct credentials SHALL return a JWT whose decoded claims contain the correct email and role matching the user's stored values.

**Validates: Requirements 1.3**

---

### Property 3: RBAC enforcement is consistent

*For any* authenticated request, the HTTP response status SHALL be 403 if and only if the authenticated user's role does not have permission for the requested endpoint, and 200/201/204 if the role does have permission (assuming the request is otherwise valid).

**Validates: Requirements 2.1, 2.2, 2.3, 2.5**

---

### Property 4: Invalid JWT tokens are always rejected

*For any* JWT token that has been tampered with (signature mismatch) or has expired, the System SHALL return HTTP 401 and SHALL NOT process the request.

**Validates: Requirements 2.4, 2.6**

---

### Property 5: Inactive users cannot authenticate

*For any* user whose status is INACTIVE, a login attempt with correct credentials SHALL return HTTP 401 and SHALL NOT issue a JWT token.

**Validates: Requirements 3.6**

---

### Property 6: Role update round-trip

*For any* existing user and any valid Role value, updating the user's role via PUT /users/{id}/role and then fetching the user SHALL return the updated role value.

**Validates: Requirements 3.2**

---

### Property 7: Record create round-trip

*For any* valid RecordRequest submitted by an ADMIN, the created FinancialRecord retrieved by its id SHALL have amount, type, category, date, and description matching the request, and createdBy SHALL reference the authenticated admin user.

**Validates: Requirements 4.1**

---

### Property 8: Soft delete excludes records from queries

*For any* FinancialRecord that has been deleted via DELETE /records/{id}, that record SHALL NOT appear in GET /records results, GET /dashboard/summary totals, or GET /dashboard/trends totals. The record SHALL still exist in the database with deleted=true.

**Validates: Requirements 4.4, 8.4**

---

### Property 9: Combined filter correctness

*For any* combination of active filter parameters (startDate, endDate, category, type), every record returned by GET /records SHALL satisfy ALL active filter conditions simultaneously (AND logic). No record failing any active filter SHALL appear in the results.

**Validates: Requirements 5.1, 5.2, 5.3, 5.4**

---

### Property 10: Dashboard summary invariant

*For any* set of non-deleted financial records, the dashboard summary SHALL satisfy:
- `totalIncome` = sum of all INCOME record amounts
- `totalExpenses` = sum of all EXPENSE record amounts
- `netBalance` = `totalIncome` - `totalExpenses`
- Each category total = sum of amounts for records in that category
- `recentTransactions` contains exactly min(5, total records) records ordered by date descending

**Validates: Requirements 6.1, 6.2, 6.4**

---

### Property 11: Monthly trends correctness

*For any* set of non-deleted financial records in the current year, each month entry in GET /dashboard/trends SHALL have `totalIncome` equal to the sum of INCOME amounts for that month and `totalExpenses` equal to the sum of EXPENSE amounts for that month.

**Validates: Requirements 6.3**

---

### Property 12: Error responses are structured and safe

*For any* exception thrown during request processing, the HTTP response body SHALL be a JSON object containing `timestamp`, `status`, and `message` fields, and SHALL NOT contain Java stack trace text (e.g., "at com.", "Exception in thread").

**Validates: Requirements 7.1, 7.4, 7.6**

---

## Error Handling

All exceptions are handled by `GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception | HTTP Status | Notes |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Returns map of field → error message |
| `DuplicateEmailException` | 400 | Email already registered |
| `ResourceNotFoundException` | 404 | User or record not found |
| `InactiveUserException` | 401 | User account is deactivated |
| `AuthenticationException` | 401 | Bad credentials |
| `AccessDeniedException` | 403 | Insufficient role |
| `Exception` (catch-all) | 500 | Generic message, no stack trace |

Error response shape:
```json
{
  "timestamp": "2026-04-02T10:30:00",
  "status": 400,
  "message": "Validation failed",
  "errors": { "email": "must be a valid email" }
}
```

---

## Testing Strategy

### Dual Testing Approach

Both unit tests and property-based tests are required. They are complementary:
- Unit tests verify specific examples, edge cases, and integration points
- Property-based tests verify universal correctness across many generated inputs

### Property-Based Testing Library

**Java library: [jqwik](https://jqwik.net/)** — a property-based testing framework for JUnit 5. It integrates naturally with Spring Boot Test and provides rich generators (`@ForAll`, `@Provide`, `Arbitraries`).

Dependency:
```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.4</version>
    <scope>test</scope>
</dependency>
```

### Property Test Configuration

- Minimum **100 tries** per property (jqwik default is 1000, which is acceptable)
- Each property test annotated with a comment referencing the design property:
  ```java
  // Feature: finance-dashboard, Property 1: Registration defaults are always correct
  // Validates: Requirements 1.1
  @Property
  void registrationDefaultsAreCorrect(@ForAll("validRegisterRequests") RegisterRequest req) { ... }
  ```

### Unit Test Coverage

- `AuthServiceTest` — register duplicate email, login bad password, login inactive user
- `UserServiceTest` — update role not found, update status not found
- `RecordServiceTest` — create record, update record, delete record, filter combinations
- `DashboardServiceTest` — summary with empty records, trends with no current-year records
- `JwtUtilTest` — token generation, expiry, tampered token rejection
- `GlobalExceptionHandlerTest` — each exception type returns correct status and shape

### Integration Tests

- `AuthControllerIT` — full register → login → use token flow
- `RecordControllerIT` — CRUD with role enforcement
- `DashboardControllerIT` — summary and trends with seeded data

### Test Isolation

- Use H2 in-memory database for all tests (`spring.datasource.url=jdbc:h2:mem:testdb`)
- Each test class annotated with `@SpringBootTest` + `@Transactional` for rollback
- Property tests use `@SpringBootTest` with `@AutoConfigureMockMvc`
