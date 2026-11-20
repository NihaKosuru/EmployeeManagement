package com.example.ems.service;

import com.example.ems.dto.EmployeeRequest;
import com.example.ems.dto.EmployeeResponse;
import com.example.ems.dto.PageResponse;
import com.example.ems.entity.Department;
import com.example.ems.entity.Employee;
import com.example.ems.exception.DuplicateResourceException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class EmployeeService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "email", "designation", "salary");
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getEmployees(String name, Long departmentId, int page, int size,
                                                       String sortBy, String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("sortBy must be one of " + SORTABLE_FIELDS);
        }
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        String nameFilter = (name == null || name.isBlank()) ? null : name.trim();

        Page<Employee> employees = employeeRepository.search(nameFilter, departmentId, PageRequest.of(page, size, sort));
        return PageResponse.from(employees.map(EmployeeResponse::from));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long id) {
        return EmployeeResponse.from(findEmployee(id));
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An employee with email " + request.email() + " already exists");
        }
        Employee employee = new Employee();
        copyRequestToEmployee(request, employee);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        if (request.version() == null) {
            throw new IllegalArgumentException("version is required when updating an employee");
        }
        Employee employee = findEmployee(id);

        // The client sends the version it last read. If someone else has saved the
        // employee since then, the versions differ and this update is rejected (409).
        if (!employee.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Employee.class, id);
        }
        if (employeeRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("An employee with email " + request.email() + " already exists");
        }
        copyRequestToEmployee(request, employee);

        // Flush now so the new version number is included in the response. If another
        // transaction committed in the meantime, Hibernate's version check fails here.
        employeeRepository.saveAndFlush(employee);
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeRepository.delete(findEmployee(id));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private void copyRequestToEmployee(EmployeeRequest request, Employee employee) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.departmentId()));
        employee.setName(request.name().trim());
        employee.setEmail(request.email().trim());
        employee.setDesignation(request.designation().trim());
        employee.setSalary(request.salary());
        employee.setDepartment(department);
    }
}
