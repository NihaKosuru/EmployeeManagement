package com.example.ems.service;

import com.example.ems.dto.EmployeeRequest;
import com.example.ems.dto.EmployeeResponse;
import com.example.ems.entity.Department;
import com.example.ems.entity.Employee;
import com.example.ems.exception.DuplicateResourceException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Department engineering;

    @BeforeEach
    void setUp() {
        engineering = new Department();
        engineering.setId(1L);
        engineering.setName("Engineering");
    }

    @Test
    void createEmployeeSavesEmployeeWithDepartment() {
        when(employeeRepository.existsByEmail("arjun.sharma@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(engineering));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setVersion(0L);
            return saved;
        });

        EmployeeResponse response = employeeService.createEmployee(request(null));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Arjun Sharma");
        assertThat(response.departmentName()).isEqualTo("Engineering");
    }

    @Test
    void createEmployeeWithExistingEmailIsRejected() {
        when(employeeRepository.existsByEmail("arjun.sharma@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(request(null)));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployeeWithUnknownDepartmentIsRejected() {
        when(employeeRepository.existsByEmail("arjun.sharma@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.createEmployee(request(null)));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void getEmployeeThatDoesNotExistThrowsNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployee(99L));
    }

    @Test
    void updateWithCurrentVersionSavesChanges() {
        Employee existing = existingEmployee(3L);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmailAndIdNot("arjun.sharma@example.com", 5L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(engineering));

        EmployeeResponse response = employeeService.updateEmployee(5L, request(3L));

        assertThat(response.designation()).isEqualTo("Senior Software Engineer");
        verify(employeeRepository).saveAndFlush(existing);
    }

    @Test
    void updateWithStaleVersionIsRejected() {
        // Another user already saved the employee, so the database version (3) is newer
        // than the version this client read (2).
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existingEmployee(3L)));

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> employeeService.updateEmployee(5L, request(2L)));
        verify(employeeRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateWithoutVersionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> employeeService.updateEmployee(5L, request(null)));
    }

    @Test
    void listingWithUnknownSortFieldIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> employeeService.getEmployees(null, null, 0, 10, "password", "asc"));
    }

    private EmployeeRequest request(Long version) {
        return new EmployeeRequest("Arjun Sharma", "arjun.sharma@example.com", "Senior Software Engineer",
                new BigDecimal("1800000"), 1L, version);
    }

    private Employee existingEmployee(Long version) {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setName("Arjun Sharma");
        employee.setEmail("arjun.sharma@example.com");
        employee.setDesignation("Software Engineer");
        employee.setSalary(new BigDecimal("1200000"));
        employee.setDepartment(engineering);
        employee.setVersion(version);
        return employee;
    }
}
