package com.example.ems.controller;

import com.example.ems.dto.AssignmentRequest;
import com.example.ems.dto.DepartmentRequest;
import com.example.ems.dto.EmployeeRequest;
import com.example.ems.dto.ProjectRequest;
import com.example.ems.entity.Department;
import com.example.ems.entity.Employee;
import com.example.ems.entity.Project;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.EmployeeRepository;
import com.example.ems.repository.ProjectAssignmentRepository;
import com.example.ems.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through HTTP: security, controllers, services, repositories and the
 * (in-memory) database all run for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectAssignmentRepository assignmentRepository;

    private Department engineering;
    private Department hr;

    @BeforeEach
    void resetDatabase() {
        assignmentRepository.deleteAll();
        employeeRepository.deleteAll();
        projectRepository.deleteAll();
        departmentRepository.deleteAll();
        engineering = saveDepartment("Engineering");
        hr = saveDepartment("Human Resources");
    }

    // ---------- Security ----------

    @Test
    void requestWithoutCredentialsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithWrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/employees").with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReadButCannotModify() throws Exception {
        mockMvc.perform(get("/api/departments").with(user())).andExpect(status().isOk());

        mockMvc.perform(post("/api/departments").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new DepartmentRequest("Finance", "Mumbai"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userGetsForbiddenForEveryChange() throws Exception {
        Employee employee = saveEmployee("Arjun Sharma", engineering);
        String body = toJson(employeeRequest("Arjun Sharma", "arjun.sharma@example.com", 0L));

        mockMvc.perform(post("/api/employees").with(user()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/employees/{id}", employee.getId()).with(user())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/employees/{id}", employee.getId()).with(user()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/assignments").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new AssignmentRequest(employee.getId(), 1L, "Developer"))))
                .andExpect(status().isForbidden());

        assertThat(employeeRepository.findById(employee.getId())).isPresent();
    }

    @Test
    void currentUserEndpointReturnsRole() throws Exception {
        mockMvc.perform(get("/api/me").with(admin()))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mockMvc.perform(get("/api/me").with(user()))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void swaggerDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Employee Management API"));
    }

    @Test
    void webPageFilesArePublic() throws Exception {
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/style.css")).andExpect(status().isOk());
        mockMvc.perform(get("/app.js")).andExpect(status().isOk());
    }

    // ---------- Employees ----------

    @Test
    void employeeCrudFlow() throws Exception {
        EmployeeRequest createRequest = employeeRequest("Arjun Sharma", "arjun.sharma@example.com", null);
        JsonNode created = readJson(mockMvc.perform(post("/api/employees").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.version").value(0)));
        long id = created.get("id").asLong();

        mockMvc.perform(get("/api/employees/{id}", id).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("arjun.sharma@example.com"));

        EmployeeRequest updateRequest = new EmployeeRequest("Arjun Sharma", "arjun.sharma@example.com",
                "Tech Lead", new BigDecimal("2500000"), hr.getId(), 0L);
        mockMvc.perform(put("/api/employees/{id}", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designation").value("Tech Lead"))
                .andExpect(jsonPath("$.departmentName").value("Human Resources"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(delete("/api/employees/{id}", id).with(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/{id}", id).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id " + id));
    }

    @Test
    void invalidEmployeeReturnsValidationErrors() throws Exception {
        EmployeeRequest invalid = new EmployeeRequest("", "not-an-email", "Engineer",
                new BigDecimal("-5"), null, null);

        mockMvc.perform(post("/api/employees").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be a valid email address"))
                .andExpect(jsonPath("$.fieldErrors.salary").value("Salary must be greater than zero"))
                .andExpect(jsonPath("$.fieldErrors.departmentId").value("Department id is required"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        saveEmployee("Arjun Sharma", engineering);

        mockMvc.perform(post("/api/employees").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(employeeRequest("Another Arjun", "arjun.sharma@example.com", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void employeesCanBeSearchedFilteredAndPaged() throws Exception {
        saveEmployee("Arjun Sharma", engineering);
        saveEmployee("Priya Reddy", engineering);
        saveEmployee("Rahul Verma", engineering);
        saveEmployee("Sneha Iyer", hr);

        mockMvc.perform(get("/api/employees").param("name", "sharma").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Arjun Sharma"));

        mockMvc.perform(get("/api/employees").param("departmentId", hr.getId().toString()).with(user()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Sneha Iyer"));

        mockMvc.perform(get("/api/employees")
                        .param("page", "1").param("size", "2")
                        .param("sortBy", "name").param("direction", "asc")
                        .with(user()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Rahul Verma"))
                .andExpect(jsonPath("$.content[1].name").value("Sneha Iyer"));

        mockMvc.perform(get("/api/employees").param("sortBy", "unknownField").with(user()))
                .andExpect(status().isBadRequest());
    }

    // ---------- Concurrency (optimistic locking) ----------

    @Test
    void staleEmployeeUpdateReturnsConflict() throws Exception {
        Employee employee = saveEmployee("Priya Reddy", engineering);

        // Both users read version 0. The first update succeeds and moves the version to 1.
        EmployeeRequest firstUserUpdate = new EmployeeRequest("Priya Reddy", "priya.reddy@example.com",
                "Senior Engineer", new BigDecimal("1500000"), engineering.getId(), 0L);
        mockMvc.perform(put("/api/employees/{id}", employee.getId()).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstUserUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        // The second user still sends version 0, so the update is rejected.
        EmployeeRequest secondUserUpdate = new EmployeeRequest("Priya Reddy", "priya.reddy@example.com",
                "Engineer", new BigDecimal("1300000"), engineering.getId(), 0L);
        mockMvc.perform(put("/api/employees/{id}", employee.getId()).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(secondUserUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This record was updated by another user. Please reload it and try again."));

        Employee stored = employeeRepository.findById(employee.getId()).orElseThrow();
        assertThat(stored.getDesignation()).isEqualTo("Senior Engineer");
    }

    @Test
    void jpaVersionCheckRejectsSecondConcurrentSave() {
        Employee saved = saveEmployee("Rahul Verma", engineering);

        // Two transactions load the same row (both at version 0)
        Employee firstCopy = employeeRepository.findById(saved.getId()).orElseThrow();
        Employee secondCopy = employeeRepository.findById(saved.getId()).orElseThrow();

        firstCopy.setSalary(new BigDecimal("2600000"));
        employeeRepository.save(firstCopy);

        secondCopy.setSalary(new BigDecimal("2700000"));
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> employeeRepository.save(secondCopy));

        Employee stored = employeeRepository.findById(saved.getId()).orElseThrow();
        assertThat(stored.getSalary()).isEqualByComparingTo("2600000");
        assertThat(stored.getVersion()).isEqualTo(1L);
    }

    // ---------- Departments ----------

    @Test
    void departmentOperations() throws Exception {
        JsonNode created = readJson(mockMvc.perform(post("/api/departments").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new DepartmentRequest("Finance", "Mumbai"))))
                .andExpect(status().isCreated()));
        long financeId = created.get("id").asLong();

        mockMvc.perform(post("/api/departments").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new DepartmentRequest("Finance", "Pune"))))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/departments/{id}", financeId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new DepartmentRequest("Finance", "Pune"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Pune"));

        mockMvc.perform(get("/api/departments").with(user()))
                .andExpect(jsonPath("$.length()").value(3));

        saveEmployee("Arjun Sharma", engineering);
        mockMvc.perform(get("/api/departments/{id}/employees", engineering.getId()).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Arjun Sharma"));

        // A department that still has employees cannot be deleted
        mockMvc.perform(delete("/api/departments/{id}", engineering.getId()).with(admin()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/departments/{id}", financeId).with(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/departments/{id}", financeId).with(user()))
                .andExpect(status().isNotFound());
    }

    // ---------- Projects ----------

    @Test
    void projectOperations() throws Exception {
        JsonNode created = readJson(mockMvc.perform(post("/api/projects").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ProjectRequest("Payroll System", "Monthly payroll", new BigDecimal("5000000")))))
                .andExpect(status().isCreated()));
        long projectId = created.get("id").asLong();

        mockMvc.perform(post("/api/projects").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ProjectRequest("Hiring Portal", null, BigDecimal.ZERO))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.budget").value("Budget must be greater than zero"));

        mockMvc.perform(put("/api/projects/{id}", projectId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ProjectRequest("Payroll System", "Payroll v2", new BigDecimal("6000000")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Payroll v2"));

        mockMvc.perform(get("/api/projects/{id}", projectId).with(user()))
                .andExpect(jsonPath("$.budget").value(6000000));

        mockMvc.perform(get("/api/projects").with(user()))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/projects/{id}", projectId).with(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", projectId).with(user()))
                .andExpect(status().isNotFound());
    }

    // ---------- Assignments and reports ----------

    @Test
    void assignmentFlowPreventsDuplicatesAndInvalidReferences() throws Exception {
        Employee priya = saveEmployee("Priya Reddy", engineering);
        Project payroll = saveProject("Payroll System");

        JsonNode assignment = readJson(mockMvc.perform(post("/api/assignments").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new AssignmentRequest(priya.getId(), payroll.getId(), "Backend Developer"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeName").value("Priya Reddy"))
                .andExpect(jsonPath("$.projectName").value("Payroll System")));

        // Same employee and project again
        mockMvc.perform(post("/api/assignments").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new AssignmentRequest(priya.getId(), payroll.getId(), "Tester"))))
                .andExpect(status().isConflict());

        // Employee that does not exist
        mockMvc.perform(post("/api/assignments").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new AssignmentRequest(99999L, payroll.getId(), "Tester"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id 99999"));

        mockMvc.perform(get("/api/projects/{id}/employees", payroll.getId()).with(user()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Priya Reddy"));

        mockMvc.perform(get("/api/assignments").with(user()))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/reports/project-summary").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectName").value("Payroll System"))
                .andExpect(jsonPath("$[0].employeeCount").value(1));

        mockMvc.perform(delete("/api/assignments/{id}", assignment.get("id").asLong()).with(admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}/employees", payroll.getId()).with(user()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void departmentSummaryReport() throws Exception {
        saveEmployee("Arjun Sharma", engineering);
        saveEmployee("Priya Reddy", engineering);

        mockMvc.perform(get("/api/reports/department-summary").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].departmentName").value("Engineering"))
                .andExpect(jsonPath("$[0].employeeCount").value(2))
                .andExpect(jsonPath("$[0].totalSalary").value(2000000))
                .andExpect(jsonPath("$[1].departmentName").value("Human Resources"))
                .andExpect(jsonPath("$[1].employeeCount").value(0));
    }

    // ---------- Helpers ----------

    private static RequestPostProcessor admin() {
        return httpBasic("admin", "admin123");
    }

    private static RequestPostProcessor user() {
        return httpBasic("user", "user123");
    }

    private EmployeeRequest employeeRequest(String name, String email, Long version) {
        return new EmployeeRequest(name, email, "Software Engineer", new BigDecimal("1200000"),
                engineering.getId(), version);
    }

    private Department saveDepartment(String name) {
        Department department = new Department();
        department.setName(name);
        return departmentRepository.save(department);
    }

    private Employee saveEmployee(String name, Department department) {
        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
        employee.setDesignation("Software Engineer");
        employee.setSalary(new BigDecimal("1000000"));
        employee.setDepartment(department);
        return employeeRepository.save(employee);
    }

    private Project saveProject(String name) {
        Project project = new Project();
        project.setName(name);
        project.setBudget(new BigDecimal("5000000"));
        return projectRepository.save(project);
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
