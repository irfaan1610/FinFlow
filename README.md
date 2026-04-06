# Finance Dashboard Backend

A Spring Boot REST API for financial data processing and access control. Supports user management with role-based access control (RBAC), financial records CRUD with filtering, and aggregated dashboard analytics.

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+ (or compatible)

---

## Setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd finance-dashboard
```

### 2. Create the database

```sql
CREATE DATABASE finance_db;
```

### 3. Configure environment

Edit `src/main/resources/application.properties` or set the environment variables listed below.

### 4. Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/finance_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `password` | Database password |
| `APP_JWT_SECRET` | *(hex string in properties)* | HMAC-SHA256 secret key for JWT signing |
| `APP_JWT_EXPIRATION_MS` | `86400000` | JWT token expiry in milliseconds (default: 24h) |

To override via environment variables, use Spring Boot's relaxed binding (e.g., `SPRING_DATASOURCE_URL` maps to `spring.datasource.url`).

---

## API Endpoints

### Authentication — public, no token required

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a new user (role defaults to VIEWER) |
| POST | `/auth/login` | Login and receive a JWT token |

### User Management — ADMIN only

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users` | List all users (paginated) |
| PUT | `/users/{id}/role` | Update a user's role |
| PATCH | `/users/{id}/status` | Activate or deactivate a user |

### Financial Records — ADMIN (write), ANALYST + ADMIN (read)

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| POST | `/records` | ADMIN | Create a financial record |
| GET | `/records` | ANALYST, ADMIN | List records (paginated, filterable) |
| PUT | `/records/{id}` | ADMIN | Update a record |
| DELETE | `/records/{id}` | ADMIN | Soft-delete a record (returns 204) |

**GET /records query parameters:**

| Parameter | Type | Description |
|---|---|---|
| `startDate` | `YYYY-MM-DD` | Filter records on or after this date |
| `endDate` | `YYYY-MM-DD` | Filter records on or before this date |
| `category` | string | Filter by category name |
| `type` | `INCOME` \| `EXPENSE` | Filter by record type |
| `page` | int | Page number (0-based) |
| `size` | int | Page size |

### Dashboard Analytics — VIEWER, ANALYST, ADMIN

| Method | Endpoint | Description |
|---|---|---|
| GET | `/dashboard/summary` | Total income, expenses, net balance, category totals, 5 recent transactions |
| GET | `/dashboard/trends` | Monthly income/expense totals for the current year |

---

## Sample curl Requests

### Register a user

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@example.com", "password": "secret123"}'
```

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com", "password": "secret123"}'
```

Response includes a `token` field. Use it as a Bearer token in subsequent requests.

### Create a financial record (ADMIN)

```bash
curl -X POST http://localhost:8080/records \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 1500.00, "type": "INCOME", "category": "Salary", "date": "2026-04-01", "description": "April salary"}'
```

### List records with filters (ANALYST or ADMIN)

```bash
curl "http://localhost:8080/records?startDate=2026-01-01&endDate=2026-04-30&type=INCOME&page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

### Get dashboard summary

```bash
curl http://localhost:8080/dashboard/summary \
  -H "Authorization: Bearer <token>"
```

### Update a user's role (ADMIN)

```bash
curl -X PUT http://localhost:8080/users/2/role \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"role": "ANALYST"}'
```

### Deactivate a user (ADMIN)

```bash
curl -X PATCH http://localhost:8080/users/2/status \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "INACTIVE"}'
```

---

## Roles Summary

| Role | Dashboard | Read Records | Write Records | User Management |
|---|---|---|---|---|
| VIEWER | ✅ | ❌ | ❌ | ❌ |
| ANALYST | ✅ | ✅ | ❌ | ❌ |
| ADMIN | ✅ | ✅ | ✅ | ✅ |

---

## Assumptions

- Newly registered users are always assigned the `VIEWER` role. Role elevation requires an ADMIN action.
- Deleted records are soft-deleted (`deleted=true`) and are excluded from all queries and analytics, but remain in the database.
- JWT tokens are stateless; there is no server-side token revocation. Deactivating a user prevents new logins but does not invalidate existing tokens until they expire.
- The database schema is managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update`). For production, consider using a migration tool like Flyway or Liquibase.
- All monetary amounts are stored as `BigDecimal` to avoid floating-point precision issues.
- Dashboard trends are scoped to the current calendar year only.
