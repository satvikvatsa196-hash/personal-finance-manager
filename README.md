# Personal Finance Manager

A comprehensive personal finance management system built with Kotlin and Spring Boot.

## Technology Stack
- Kotlin
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- PostgreSQL
- JUnit 5 & Mockito
- Gradle Kotlin DSL

## Setup

1. **Database Setup**
   Ensure PostgreSQL is running. Create a database named `finance_manager`.
   You can configure the database credentials using environment variables:
   - `DB_URL` (default: `jdbc:postgresql://localhost:5432/finance_manager`)
   - `DB_USERNAME` (default: `postgres`)
   - `DB_PASSWORD` (default: `postgres`)

2. **Build the Project**
   ```bash
   ./gradlew build
   ```

3. **Run the Application**
   ```bash
   ./gradlew bootRun
   ```

## Architecture
- Layered Architecture: Controller -> Service -> Repository
- DTOs used for all API endpoints

## Testing
To run the tests:
```bash
./gradlew test
```
