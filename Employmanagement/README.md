# Employee Management API

A REST API built with Spring Boot for managing employees, departments, projects and project assignments.
It supports search, filtering, pagination, reports, role-based security and optimistic locking.

## Technology

Java 17, Spring Boot 3.5.6, Maven, Spring Web, Spring Data JPA (Hibernate), MySQL 8, Spring Security (BCrypt),
Jakarta Bean Validation, Springdoc OpenAPI (Swagger UI), JUnit 5, Mockito, Spring Boot Test, MockMvc.
Tests use an in-memory H2 database, so you don't need MySQL to run them.

## Project structure

```
Employmanagement
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/            Maven wrapper (no Maven install needed)
├── database/schema.sql              MySQL tables + sample data
├── PERFORMANCE_REPORT.md            N+1 problem and query optimization
└── src
    ├── main/java/com/example/ems
    │   ├── EmployeeManagementApplication.java   (also holds the OpenAPI definition)
    │   ├── config/SecurityConfig.java
    │   ├── controller/   EmployeeController, DepartmentController, ProjectController,
    │   │                 AssignmentController, ReportController
    │   ├── entity/       Employee, Department, Project, ProjectAssignment
    │   ├── dto/          request/response records, PageResponse, report rows, ApiError
    │   ├── repository/   Spring Data JPA repositories
    │   ├── service/      business logic and transactions
    │   └── exception/    GlobalExceptionHandler, ResourceNotFoundException, DuplicateResourceException
    ├── main/resources/application.properties
    └── test
        ├── java/com/example/ems
        │   ├── controller/ApiIntegrationTest.java       (SpringBootTest + MockMvc)
        │   ├── repository/QueryOptimizationTest.java    (DataJpaTest, N+1 demo, DB constraint)
        │   └── service/EmployeeServiceTest.java, AssignmentServiceTest.java  (Mockito)
        └── resources/application.properties             (H2 settings)
```

## 1. Database setup

1. Make sure MySQL 8 is running.
2. Create the database, tables and sample data:

   ```
   mysql -u root -p < database/schema.sql
   ```

   This creates the `employee_db` database. Running it again drops and recreates the tables.

3. By default the app connects as `root` / `root`. To use other credentials, set environment variables:

   ```
   # Windows PowerShell
   $env:DB_USERNAME="myuser"; $env:DB_PASSWORD="mypassword"
   # Linux / macOS
   export DB_USERNAME=myuser DB_PASSWORD=mypassword
   ```

   `DB_URL` can also be set (default `jdbc:mysql://localhost:3306/employee_db`).

Hibernate runs with `ddl-auto=validate`: it checks that the entities match the tables but never changes the schema.

## 2. Run the application

```
# Windows
mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run
```

If port 8080 is already in use, add `-Dspring-boot.run.arguments=--server.port=8081`.

Or build a jar and run it:

```
mvnw.cmd clean package
java -jar target/employee-management-api-1.0.0.jar
```

## 3. Web page

Open **http://localhost:8080/** to use the simple web page (plain HTML, CSS and JavaScript in
`src/main/resources/static`). Log in with one of the users below. The page has tabs for Employees
(search, filter, paging, add, edit, delete), Departments, Projects, Assignments and Reports.
API errors (validation, 403, 409 conflicts) are shown at the top of the page.

## Swagger UI

Open **http://localhost:8080/swagger-ui.html** and click **Authorize** to enter a username and password.
The OpenAPI JSON is at http://localhost:8080/v3/api-docs.

## 4. Users and roles

| Username | Password  | Role  | Access                                  |
|----------|-----------|-------|-----------------------------------------|
| admin    | admin123  | ADMIN | Everything                              |
| user     | user123   | USER  | Read only (GET requests)                |

Passwords are stored as BCrypt hashes in an in-memory user store. All `/api/**` endpoints need HTTP Basic authentication.
Without credentials you get `401`, and a USER trying to change data gets `403`.

## 5. Run the tests

```
mvnw.cmd test
```

There are 35 tests in 4 classes. They cover employee CRUD, departments, projects, assignments, duplicate assignment prevention,
validation, security, optimistic locking, pagination/filtering, reports and the N+1 query fix.

## 6. API endpoints

| Method | URL | Role | Description |
|--------|-----|------|-------------|
| GET    | /api/employees | USER | List employees. Params: `name`, `departmentId`, `page` (0), `size` (10, max 100), `sortBy` (id, name, email, designation, salary), `direction` (asc/desc) |
| GET    | /api/employees/{id} | USER | Get one employee |
| POST   | /api/employees | ADMIN | Create employee |
| PUT    | /api/employees/{id} | ADMIN | Update employee (must include `version`) |
| DELETE | /api/employees/{id} | ADMIN | Delete employee and their assignments |
| GET    | /api/departments | USER | List departments |
| GET    | /api/departments/{id} | USER | Get one department |
| GET    | /api/departments/{id}/employees | USER | Employees of a department |
| POST   | /api/departments | ADMIN | Create department |
| PUT    | /api/departments/{id} | ADMIN | Update department |
| DELETE | /api/departments/{id} | ADMIN | Delete department (only if it has no employees) |
| GET    | /api/projects | USER | List projects |
| GET    | /api/projects/{id} | USER | Get one project |
| GET    | /api/projects/{id}/employees | USER | Employees assigned to a project |
| POST   | /api/projects | ADMIN | Create project |
| PUT    | /api/projects/{id} | ADMIN | Update project |
| DELETE | /api/projects/{id} | ADMIN | Delete project and its assignments |
| GET    | /api/assignments | USER | List assignments |
| POST   | /api/assignments | ADMIN | Assign an employee to a project |
| DELETE | /api/assignments/{id} | ADMIN | Remove an employee from a project |
| GET    | /api/reports/department-summary | USER | Employee count and total salary per department |
| GET    | /api/reports/project-summary | USER | Budget and employee count per project |
| GET    | /api/me | USER | Logged-in username and role (used by the web page) |

### Example requests (curl)

```
curl -u user:user123 "http://localhost:8080/api/employees?name=sharma"
curl -u user:user123 "http://localhost:8080/api/employees?departmentId=1&page=0&size=5&sortBy=salary&direction=desc"

curl -u admin:admin123 -X POST http://localhost:8080/api/employees -H "Content-Type: application/json" \
  -d '{"name":"Meera Joshi","email":"meera.joshi@example.com","designation":"QA Engineer","salary":900000,"departmentId":1}'

curl -u admin:admin123 -X POST http://localhost:8080/api/assignments -H "Content-Type: application/json" \
  -d '{"employeeId":5,"projectId":2,"role":"Recruitment Lead"}'
```

## 7. Error responses

`GlobalExceptionHandler` returns all errors in the same JSON format:

```json
{
  "timestamp": "2025-05-01T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/employees",
  "fieldErrors": { "email": "Email must be a valid email address" }
}
```

| Situation | Status |
|-----------|--------|
| Validation error, bad JSON, invalid parameter | 400 |
| Not logged in / wrong password | 401 |
| USER tries to change data | 403 |
| Employee, department, project or assignment not found | 404 |
| Duplicate email, department name, project name or assignment | 409 |
| Stale update (optimistic locking) | 409 |
| Unexpected error | 500 |

## 8. Concurrency (optimistic locking)

`Employee`, `Department` and `Project` have a `@Version` column. Every update increases it by one.

1. Users A and B both read employee 2 and get `"version": 0`.
2. A sends `PUT /api/employees/2` with `"version": 0`. It succeeds and the response shows `"version": 1`.
3. B sends `PUT /api/employees/2` with `"version": 0`. The service sees that the version is out of date and returns
   **409 Conflict**: `"This record was updated by another user. Please reload it and try again."`

If two updates reach the database at the same moment, Hibernate's version check (`UPDATE ... WHERE id=? AND version=?`)
rejects the second one with `ObjectOptimisticLockingFailureException`. The handler also maps that to 409.
`ApiIntegrationTest.staleEmployeeUpdateReturnsConflict` and `jpaVersionCheckRejectsSecondConcurrentSave` test both cases.

## 9. Duplicate assignment prevention

- The service checks `existsByEmployeeIdAndProjectId` first and returns 409 with a clear message.
- The table also has the unique constraint `uk_employee_project (employee_id, project_id)`. If two requests pass the check
  at the same time, the database rejects the second insert. The resulting `DataIntegrityViolationException` is returned as 409.
- The foreign keys and the service's lookups reject assignments to employees or projects that don't exist (404).

## 10. Requirement mapping

| Requirement | Implementation |
|-------------|----------------|
| Employee CRUD, search, filter, paging, sorting | `EmployeeController`, `EmployeeService`, `EmployeeRepository.search` |
| Department management + employees of a department | `DepartmentController`, `DepartmentService` |
| Project management + employees on a project | `ProjectController`, `ProjectService`, `EmployeeRepository.findByProjectWithDepartment` |
| Project assignment, no duplicates, no invalid references | `AssignmentService`, unique constraint on `ProjectAssignment`, foreign keys |
| Relationships | `Department` 1-N `Employee`; `Employee` N-N `Project` through `ProjectAssignment` |
| Optimistic locking, 409 | `@Version` fields, `EmployeeService.updateEmployee`, `GlobalExceptionHandler` |
| Validation | Bean Validation annotations on the request records, `@Valid` in controllers |
| Global exception handling | `exception/GlobalExceptionHandler` |
| Transactions | `@Transactional` on service methods (read-only for queries) |
| Security | `config/SecurityConfig`: HTTP Basic, BCrypt, ADMIN/USER roles |
| Query optimization | JOIN FETCH queries and DTO projections, see `PERFORMANCE_REPORT.md` |
| Reports | `ReportController`, `ReportService`, JPQL constructor queries |
| Swagger | springdoc + annotations on the controllers |
| Tests | `src/test/java` (35 tests) |
| Web page | `src/main/resources/static` (index.html, style.css, app.js) |
| SQL schema | `database/schema.sql` |
