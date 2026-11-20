package com.example.ems.repository;

import com.example.ems.dto.DepartmentSummary;
import com.example.ems.entity.Department;
import com.example.ems.entity.Employee;
import com.example.ems.entity.Project;
import com.example.ems.entity.ProjectAssignment;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Shows the N+1 query problem and the JOIN FETCH fix by counting the SQL statements
 * Hibernate actually runs. Also checks the repository queries and the unique constraint.
 */
@DataJpaTest
class QueryOptimizationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProjectAssignmentRepository assignmentRepository;

    private Statistics statistics;
    private Department engineering;
    private Department finance;

    @BeforeEach
    void setUp() {
        statistics = entityManager.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();

        engineering = persistDepartment("Engineering");
        Department hr = persistDepartment("Human Resources");
        finance = persistDepartment("Finance");

        persistEmployee("Arjun Sharma", engineering, "1800000");
        persistEmployee("Priya Reddy", engineering, "1200000");
        persistEmployee("Sneha Iyer", hr, "1500000");
        persistEmployee("Karthik Nair", hr, "800000");
        persistEmployee("Ananya Gupta", finance, "1100000");
        persistEmployee("Rahul Verma", finance, "2400000");

        // Start each test with an empty persistence context, like a new HTTP request
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }

    @Test
    void findAllCausesNPlusOneQueriesWhenDepartmentIsRead() {
        List<Employee> employees = employeeRepository.findAll();
        employees.forEach(employee -> employee.getDepartment().getName());

        // 1 query for the employees + 1 query for each of the 3 departments
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    @Test
    void searchWithJoinFetchLoadsEmployeesAndDepartmentsInOneQuery() {
        Page<Employee> page = employeeRepository.search(null, null, PageRequest.of(0, 10, Sort.by("name")));
        page.forEach(employee -> employee.getDepartment().getName());

        assertThat(page.getContent()).hasSize(6);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void searchFiltersByNameAndDepartmentWithPaging() {
        Page<Employee> byName = employeeRepository.search("sharma", null, PageRequest.of(0, 10));
        assertThat(byName.getContent()).extracting(Employee::getName).containsExactly("Arjun Sharma");

        Page<Employee> byDepartment = employeeRepository.search(null, finance.getId(),
                PageRequest.of(0, 1, Sort.by("name")));
        assertThat(byDepartment.getTotalElements()).isEqualTo(2);
        assertThat(byDepartment.getTotalPages()).isEqualTo(2);
        assertThat(byDepartment.getContent()).extracting(Employee::getName).containsExactly("Ananya Gupta");
    }

    @Test
    void departmentSummaryIsCalculatedByTheDatabase() {
        List<DepartmentSummary> summary = departmentRepository.getDepartmentSummary();

        assertThat(summary).hasSize(3);
        DepartmentSummary engineeringRow = summary.stream()
                .filter(row -> row.departmentName().equals("Engineering"))
                .findFirst()
                .orElseThrow();
        assertThat(engineeringRow.employeeCount()).isEqualTo(2);
        assertThat(engineeringRow.totalSalary()).isEqualByComparingTo("3000000");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void databaseRejectsDuplicateAssignment() {
        Employee employee = employeeRepository.search("Priya", engineering.getId(), PageRequest.of(0, 1))
                .getContent().get(0);
        Project project = new Project();
        project.setName("Payroll System");
        project.setBudget(new BigDecimal("5000000"));
        entityManager.persist(project);

        assignmentRepository.saveAndFlush(newAssignment(employee, project));

        // Bypasses the service check on purpose: the unique constraint is the last line of defence
        assertThrows(DataIntegrityViolationException.class,
                () -> assignmentRepository.saveAndFlush(newAssignment(employee, project)));
    }

    private Department persistDepartment(String name) {
        Department department = new Department();
        department.setName(name);
        return entityManager.persist(department);
    }

    private void persistEmployee(String name, Department department, String salary) {
        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
        employee.setDesignation("Engineer");
        employee.setSalary(new BigDecimal(salary));
        employee.setDepartment(department);
        entityManager.persist(employee);
    }

    private ProjectAssignment newAssignment(Employee employee, Project project) {
        ProjectAssignment assignment = new ProjectAssignment();
        assignment.setEmployee(employee);
        assignment.setProject(project);
        assignment.setRole("Developer");
        assignment.setAssignedDate(LocalDate.now());
        return assignment;
    }
}
