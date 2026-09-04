# Personal Finance Manager API

## 1. Overview
The **Personal Finance Manager** is a robust, production-ready RESTful API built to help users manage their financial transactions, categorize expenses/income, set savings goals, and generate analytical reports. Designed as a strict assignment implementation, it emphasizes clean code, secure data isolation, robust input validation, and high test coverage.

## 2. Features
* **User Management**: Secure registration and session-based login.
* **Category Management**: 7 immutable system defaults + unlimited user-defined custom categories.
* **Transaction Tracking**: Granular tracking of income and expenses with date, category, and advanced filtering.
* **Savings Goals**: Dynamic real-time calculation of savings progress based on income vs. expenses since the goal's inception.
* **Reporting Engine**: Monthly and yearly aggregation reports resolving net savings.
* **Strict Security**: Absolute cross-user data isolation—users can only query, modify, or delete their own financial records.

## 3. Technology Stack
* **Language**: Kotlin (JVM 21)
* **Framework**: Spring Boot 3.x
* **Security**: Spring Security (Session-Based Auth)
* **Database**: PostgreSQL
* **ORM**: Spring Data JPA / Hibernate
* **Build Tool**: Gradle Kotlin DSL
* **Testing**: JUnit 5, Mockito, MockMvc, JaCoCo
* **API Documentation**: OpenAPI (Swagger UI)

## 4. Architecture
The application strictly follows a standard Layered Architecture:
* **Controllers**: Handles HTTP routing, request/response DTO mapping, and authentication bindings.
* **Services**: Houses core business logic, math calculations, and cross-entity validations.
* **Repositories**: Manages PostgreSQL database interactions using Spring Data JPA and JPQL aggregations.

## 5. Package Structure
```text
src/main/kotlin/com/example/financemanager
├── config         // Spring Security, Swagger, and Data Initialization
├── controller     // REST API Endpoints
├── dto            // Immutable Request/Response Data Transfer Objects
├── exception      // Global @ControllerAdvice and custom HTTP Exceptions
├── model          // JPA Entities (User, Category, Transaction, Goal) & Enums
├── repository     // Spring Data JPA Interfaces
├── security       // UserPrincipal and UserDetailsService implementation
└── service        // Core Business Logic
```

## 6. Database Design
* **Users Table**: Stores user credentials and hashed passwords.
* **Categories Table**: Stores globally available default categories (`user_id = null`) and isolated custom categories (`user_id = X`).
* **Transactions Table**: Maps amounts to specific categories and owners.
* **Savings Goals Table**: Tracks user goals, target amounts, and start/target dates.
*(All foreign keys are properly indexed, and constraints enforce uniqueness for custom categories per user).*

## 7. Authentication/Session Architecture
**This application intentionally uses Session-Based Authentication instead of JWT**, adhering strictly to assignment constraints. 
* Upon successful login, the server establishes an `HttpSession` and returns a `JSESSIONID` cookie securely.
* The cookie is flagged as `HttpOnly`, `SameSite=Strict`, and `Secure` to prevent Cross-Site Scripting (XSS) and Cross-Site Request Forgery (CSRF).
* Subsequent API requests must include this cookie. 
* Logging out immediately invalidates the server-side session and instructs the client to delete the cookie.

## 8. API Endpoints

### Auth
* `POST /api/auth/register` - Register a new user
* `POST /api/auth/login` - Authenticate and establish a session
* `POST /api/auth/logout` - Destroy session

### Categories
* `GET /api/categories` - List default + user's custom categories
* `POST /api/categories` - Create custom category
* `DELETE /api/categories/{name}` - Delete custom category (if unreferenced)

### Transactions
* `POST /api/transactions` - Create transaction
* `GET /api/transactions` - Filter, sort, and list transactions
* `PUT /api/transactions/{id}` - Update amount, category, or description
* `DELETE /api/transactions/{id}` - Remove transaction

### Savings Goals
* `POST /api/goals` - Create goal
* `GET /api/goals` - List all goals
* `GET /api/goals/{id}` - Retrieve specific goal math and progress
* `PUT /api/goals/{id}` - Update target amount or date
* `DELETE /api/goals/{id}` - Remove goal

### Reports
* `GET /api/reports/monthly/{year}/{month}` - Monthly aggregation
* `GET /api/reports/yearly/{year}` - Yearly aggregation

## 9. Request Examples

**Create Transaction (`POST /api/transactions`)**
```json
{
  "amount": 50000.00,
  "date": "2024-01-15",
  "category": "Salary",
  "description": "January Salary"
}
```

**Partial Update Transaction (`PUT /api/transactions/{id}`)**
```json
{
  "amount": 55000.00
}
```
*(Omitted fields like category or description are safely ignored and preserved).*

## 10. Response Examples

**Goal Progress (`GET /api/goals/1`)**
```json
{
  "id": 1,
  "goalName": "Emergency Fund",
  "targetAmount": 10000.00,
  "targetDate": "2025-12-31",
  "startDate": "2024-01-01",
  "currentProgress": 5000.00,
  "progressPercentage": 50.00,
  "remainingAmount": 5000.00
}
```

## 11. Validation Rules
* **Amounts**: Must be strictly `@Positive`.
* **Dates**: Transaction dates must be `@PastOrPresent`. Goal target dates must be `@Future`.
* **Unique Names**: Custom categories cannot share a name with another custom category owned by the user, nor a system default category.
* **Foreign Keys**: You cannot delete a category if a transaction is currently using it.
* **Partial Updates**: `PUT` endpoints natively support partial updates. Fields omitted from the JSON payload bypass validation and preserve their existing values in the database.

## 12. Error Responses
All errors are gracefully intercepted and formatted to prevent stack trace leakage:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["amount: Amount must be positive"]
}
```

## 13. HTTP Status Codes
* `200 OK` - Standard success / Read / Update / Delete
* `201 Created` - Resource created successfully
* `400 Bad Request` - Validation failures / Business rule violations
* `401 Unauthorized` - Unauthenticated access or invalid credentials
* `403 Forbidden` - Attempting to modify immutable defaults
* `404 Not Found` - Resource doesn't exist (or belongs to another user)
* `409 Conflict` - Resource collision (e.g., duplicate category name)

## 14. Local Setup & 15. PostgreSQL Setup
1. Install **PostgreSQL** locally (or run via Docker).
2. Create a blank database:
   ```sql
   CREATE DATABASE finance_manager;
   ```

## 16. Environment Variables
The application relies on externalized configuration. Set these environment variables before running:
* `DB_URL` - `jdbc:postgresql://localhost:5432/finance_manager`
* `DB_USERNAME` - your postgres username
* `DB_PASSWORD` - your postgres password

## 17. Running the Application
Ensure the database is running, then execute:
```bash
./gradlew bootRun
```
*The `DataInitializer` will automatically seed the 7 default categories on the first startup.*

## 18. Running Tests
**Unit & Integration Tests**
```bash
./gradlew test
```

**E2E Bash Evaluation**
To validate the live or local API against the comprehensive test suite:
```bash
chmod +x financial_manager_tests.sh
./financial_manager_tests.sh https://your-deployment-url.onrender.com/api
```

## 19. Test Coverage
The suite utilizes **JaCoCo** to enforce an absolute minimum of **80% line coverage**. 
Run coverage generation and verification using:
```bash
./gradlew check
```
*Coverage reports are generated at `build/reports/jacoco/test/html/index.html`.*

## 20. API Testing (Swagger)
Interactive OpenAPI documentation is dynamically generated. While the application is running, navigate to:
* `http://localhost:8080/swagger-ui.html`

## 21. Deployment
The application is fully containerized and configured for automated deployment on cloud platforms like **Render**.

1. **Docker Multi-Stage Build**: The included `Dockerfile` uses the official `gradle:8.7.0-jdk21` image for compilation and the lightweight `eclipse-temurin:21-jre-jammy` image for execution.
2. **Environment Variables**: Configure the cloud service with the same `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` variables pointing to your managed PostgreSQL instance.
3. **Local Docker Execution**:
   ```bash
   docker build -t finance-manager .
   docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... finance-manager
   ```

## 22. Design Decisions
* **JPA Projections for Analytics**: Instead of fetching thousands of `Transaction` entities into memory to calculate monthly reports, the `ReportService` leverages JPQL `GROUP BY` aggregations to offload math natively to the PostgreSQL database.
* **Smart Partial Updates**: Modifying entities via `PUT` leverages nullable fields in the DTO layer (`TransactionUpdateRequest`, `GoalUpdateRequest`). The service layer intelligently checks for null values and selectively applies updates, providing PATCH-like flexibility while maintaining immutable constraints (like transaction dates).
* **DTO Shielding**: JPA Entities strictly never cross the Controller layer. They are intercepted by services and mapped to DTOs to prevent accidental serialization of sensitive data (like password hashes).

## 23. Security Considerations
* **Data Isolation**: User IDs are never trusted from client requests. All modifications and queries tightly bind the authenticated `UserPrincipal` directly to the execution flow (`WHERE t.user = :user`), structurally eliminating IDOR (Insecure Direct Object Reference).
* **CSRF Mitigation**: As a session-based API, CSRF is mitigated via `same-site: strict` and `secure: true` cookie enforcement.
* **Password Hashing**: Passwords are mathematically secured via `BCryptPasswordEncoder` with default iteration strength. 
