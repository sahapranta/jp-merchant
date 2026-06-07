# Merchant Portal Agent Guide

## Essential Commands
- Build: `mvn clean install -DskipTests`
- Run: `mvn spring-boot:run`
- Run tests: `mvn test`
- Access H2 Console: http://localhost:8080/h2-console (JDBC: jdbc:h2:mem:edubilldb)

## Project Structure
- Main entry: `MerchantApplication.java`
- Controllers: `src/main/java/com/jpay/merchant/controller/`
- Services: `src/main/java/com/jpay/merchant/service/`
- Repositories: `src/main/java/com/jpay/merchant/repository/`
- Entities: `src/main/java/com/jpay/merchant/domain/entity/`
- DTOs: `src/main/java/com/jpay/merchant/dto/`
- Config: `src/main/java/com/jpay/merchant/config/`
- Templates: `src/main/resources/templates/`
- Schema/Data: `src/main/resources/schema.sql`, `data.sql`

## Key Configuration
- Database: H2 dev (jdbc:h2:mem:edubilldb;MODE=MySQL)
- Port: 8080 (server.port=8080)
- Thymeleaf: Templates under src/main/resources/templates/ (cache=false)
- Lombok: Enable annotation processing in IDE
- SQL Init: schema.sql + data.sql load on startup (spring.sql.init.mode=always)

## Database Schema Notes
- H2 schema in schema.sql includes Oracle migration comments
- Oracle migration: BIGINT→NUMBER(19), VARCHAR→VARCHAR2, BOOLEAN→NUMBER(1)
- Remove AUTO_INCREMENT, add sequences + triggers for Oracle
- When switching to Oracle: set spring.jpa.hibernate.ddl-auto=validate

## Oracle Migration Steps
1. Uncomment Oracle JDBC dependency in pom.xml, comment out H2
2. Update application.properties:
   - spring.datasource.url=jdbc:oracle:thin:@//host:1521/XEPDB1
   - spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
   - spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
   - spring.jpa.hibernate.ddl-auto=validate
3. Run Oracle schema (create schema-oracle.sql from schema.sql with conversions)

## Testing
- Test location: src/test/java/
- Current tests: Basic context load only (MerchantApplicationTests.java)
- Add tests following Spring Boot testing conventions

## Code Conventions
- Java 21 (pom.xml properties)
- Lombok for getters/setters/constructors
- Standard Spring Boot project structure
- DTO pattern for request/response handling
- Package structure: com.jpay.merchant (not com.edubill as in README)