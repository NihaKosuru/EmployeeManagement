package com.example.ems.service;

import com.example.ems.dto.AssignmentRequest;
import com.example.ems.dto.AssignmentResponse;
import com.example.ems.entity.Employee;
import com.example.ems.entity.Project;
import com.example.ems.entity.ProjectAssignment;
import com.example.ems.exception.DuplicateResourceException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.EmployeeRepository;
import com.example.ems.repository.ProjectAssignmentRepository;
import com.example.ems.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private ProjectAssignmentRepository assignmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private Employee employee;
    private Project project;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setName("Priya Reddy");

        project = new Project();
        project.setId(2L);
        project.setName("Payroll System");
    }

    @Test
    void assignEmployeeCreatesAssignment() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(assignmentRepository.existsByEmployeeIdAndProjectId(1L, 2L)).thenReturn(false);
        when(assignmentRepository.saveAndFlush(any(ProjectAssignment.class))).thenAnswer(invocation -> {
            ProjectAssignment saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        AssignmentResponse response = assignmentService.assignEmployee(new AssignmentRequest(1L, 2L, "Developer"));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.employeeName()).isEqualTo("Priya Reddy");
        assertThat(response.projectName()).isEqualTo("Payroll System");
        assertThat(response.assignedDate()).isNotNull();
    }

    @Test
    void duplicateAssignmentIsRejected() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(assignmentRepository.existsByEmployeeIdAndProjectId(1L, 2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> assignmentService.assignEmployee(new AssignmentRequest(1L, 2L, "Developer")));
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignmentForUnknownEmployeeIsRejected() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> assignmentService.assignEmployee(new AssignmentRequest(1L, 2L, "Developer")));
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignmentForUnknownProjectIsRejected() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> assignmentService.assignEmployee(new AssignmentRequest(1L, 2L, "Developer")));
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void removingUnknownAssignmentThrowsNotFound() {
        when(assignmentRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> assignmentService.removeAssignment(50L));
    }
}
