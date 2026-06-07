# Merchant Portal — Setup Guide

## 1. Spring Initializr (start.spring.io)

### Settings
| Field        | Value                                |
|-------------|--------------------------------------|
| Project      | Maven                                |
| Language     | Java                                 |
| Spring Boot  | 3.2.x                                |
| Group        | com.edubill                          |
| Artifact     | merchant-portal                      |
| Name         | merchant-portal                      |
| Package name | com.edubill.merchantportal           |
| Packaging    | Jar                                  |
| Java         | 17                                   |

### Dependencies to select
| Dependency              | Purpose                              |
|------------------------|--------------------------------------|
| Spring Web             | MVC, REST, Thymeleaf serving         |
| Thymeleaf              | Server-side HTML templates           |
| Spring Data JPA        | ORM / Repository layer               |
| H2 Database            | In-memory DB for dev                 |
| Spring Validation      | @Valid, @NotBlank etc.               |
| Lombok                 | Boilerplate reduction                |
| Spring Security        | Auth (basic setup, expand later)     |
| Spring Boot DevTools   | Hot reload during development        |
| Spring Boot Actuator   | Health checks (optional now)         |

Download the zip, extract, open in IntelliJ / VS Code.

---

## 2. Project Structure

```
merchant-portal/
├── src/main/java/com/edubill/merchantportal/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── DataInitializer.java          ← seeds H2 with demo data
│   ├── controller/
│   │   ├── BillController.java
│   │   ├── AccountController.java
│   │   └── DashboardController.java
│   ├── domain/
│   │   ├── entity/                       ← JPA @Entity classes
│   │   └── enums/                        ← Java enums matching DB CHECK constraints
│   ├── dto/
│   │   ├── request/                      ← form-binding DTOs
│   │   └── response/                     ← API response shapes
│   ├── repository/                       ← Spring Data JPA interfaces
│   ├── service/
│   │   └── impl/                         ← business logic
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   └── util/
│       └── BillCodeGenerator.java
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql                        ← H2 DDL (auto-run on startup)
│   ├── data.sql                          ← H2 seed data
│   └── templates/
│       ├── fragments/
│       │   ├── head.html
│       │   ├── topbar.html
│       │   └── footer.html
│       └── bill/
│           ├── create.html               ← bill creation form
│           └── list.html
└── pom.xml
```

---

## 3. application.properties (H2 dev profile)

```properties
# Server
server.port=8080
server.servlet.context-path=/

# H2 In-memory DB
spring.datasource.url=jdbc:h2:mem:edubilldb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=none          # we manage schema via schema.sql
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 Console (http://localhost:8080/h2-console)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Run schema.sql + data.sql on startup
spring.sql.init.mode=always

# Thymeleaf
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# File upload (for CSV/Excel student list)
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging
logging.level.com.edubill=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

---

## 4. Oracle migration (later)

When switching from H2 to Oracle 19c:

1. Add Oracle JDBC dependency in pom.xml
2. Change datasource URL:
   ```
   spring.datasource.url=jdbc:oracle:thin:@//host:1521/XEPDB1
   spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
   spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
   spring.jpa.hibernate.ddl-auto=validate   # validate against existing schema
   ```
3. Run the Oracle SQL schema (separate file `schema-oracle.sql`)
4. Remove H2 dependency, add Oracle JDBC

---

## 5. Quick Start

```bash
# 1. Clone / extract project
cd merchant-portal

# 2. Build
mvn clean install -DskipTests

# 3. Run
mvn spring-boot:run

# 4. Open browser
http://localhost:8080/bills/create
http://localhost:8080/h2-console   (JDBC URL: jdbc:h2:mem:edubilldb)
```