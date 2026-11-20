# Performance and Query Optimization Report

## 1. The N+1 query problem

`Employee.department` is a `@ManyToOne(fetch = LAZY)` relationship. Each employee response includes the
department name (`EmployeeResponse.from` calls `employee.getDepartment().getName()`).

With a plain repository call:

```java
List<Employee> employees = employeeRepository.findAll();          // 1 query
employees.forEach(e -> e.getDepartment().getName());              // +1 query per department
```

Hibernate runs:

```sql
select ... from employees;                         -- 1 query
select ... from departments where id = 1;          -- N extra queries,
select ... from departments where id = 2;          -- one for each different
select ... from departments where id = 3;          -- department
```

With 3 departments that's **4 queries**. On a page with employees from 50 departments it would be 51 queries,
one database round trip each. This is the N+1 problem.

## 2. The fix: JOIN FETCH

`EmployeeRepository.search` loads the employees and their departments in a single query:

```java
@Query(value = """
        SELECT e FROM Employee e JOIN FETCH e.department d
        WHERE (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:departmentId IS NULL OR d.id = :departmentId)
        """,
        countQuery = "...")
Page<Employee> search(String name, Long departmentId, Pageable pageable);
```

Generated SQL (one statement):

```sql
select e.*, d.* from employees e
join departments d on d.id = e.department_id
where ... order by e.name limit ?, ?
```

- `JOIN FETCH` on a `@ManyToOne` is safe with pagination. Each employee is still one row, so `LIMIT` works
  in the database rather than in memory.
- The separate `countQuery` doesn't fetch the department, so the count stays cheap. Spring Data also skips the count
  query when the first page isn't full.

The same approach is used for:

| Method | Purpose |
|--------|---------|
| `EmployeeRepository.findByDepartmentWithDepartment` | `GET /api/departments/{id}/employees` |
| `EmployeeRepository.findByProjectWithDepartment` | `GET /api/projects/{id}/employees` |
| `ProjectAssignmentRepository.findAllWithEmployeeAndProject` | `GET /api/assignments` (fetches employee **and** project, avoiding 2N extra queries) |

## 3. Measured result

`QueryOptimizationTest` turns on Hibernate statistics and counts the SQL statements that actually run
(3 departments, 6 employees):

| Test | Approach | SQL statements |
|------|----------|----------------|
| `findAllCausesNPlusOneQueriesWhenDepartmentIsRead` | `findAll()` + lazy loading | **4** (1 + N) |
| `searchWithJoinFetchLoadsEmployeesAndDepartmentsInOneQuery` | `search()` with JOIN FETCH | **1** |

The number of queries no longer grows with the number of departments on the page.

## 4. Reports use DTO projections

Building the department report the simple way would load every department, then every department's employees
(N+1 again), and add up salaries in Java. Instead, the database does the aggregation and returns only the result rows:

```java
@Query("""
        SELECT new com.example.ems.dto.DepartmentSummary(d.id, d.name, COUNT(e.id), COALESCE(SUM(e.salary), 0))
        FROM Department d LEFT JOIN d.employees e
        GROUP BY d.id, d.name
        ORDER BY d.name
        """)
List<DepartmentSummary> getDepartmentSummary();
```

- One query, whatever the number of departments or employees (checked in `departmentSummaryIsCalculatedByTheDatabase`).
- No entities are loaded into the persistence context, so there's no dirty checking and less memory use.
- `ProjectRepository.getProjectSummary` works the same way for project assignment counts.

## 5. Indexes

| Index | Used by |
|-------|---------|
| `uk_employee_email` (unique) | `existsByEmail` check on create/update |
| `idx_employee_name` | sorting by name |
| `idx_employee_department` / FK `department_id` | department filter, department employees, report join |
| `uk_employee_project (employee_id, project_id)` | duplicate assignment check, and it prevents duplicates |
| `idx_assignment_project` | `GET /api/projects/{id}/employees`, project report |
| `uk_department_name`, `uk_project_name` | duplicate name checks |

Note: the name search uses `LIKE '%text%'`, which can't use the name index because of the leading wildcard.
That's fine for the size of this application. For very large tables a MySQL FULLTEXT index would be the next step.

## 6. Other settings

- `spring.jpa.open-in-view=false`: lazy loading can't happen silently in the web layer. All data a response needs
  is loaded inside the service's transaction, so N+1 problems show up early.
- `@Transactional(readOnly = true)` on query methods: Hibernate skips dirty checking for these reads.
- Page size is limited to 100, so one request can't load the whole table.
- To see the SQL yourself, set `spring.jpa.show-sql=true` in `application.properties`.
