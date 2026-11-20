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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssignmentService {

    private final ProjectAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    public AssignmentService(ProjectAssignmentRepository assignmentRepository,
                             EmployeeRepository employeeRepository,
                             ProjectRepository projectRepository) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments() {
        return assignmentRepository.findAllWithEmployeeAndProject().stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @Transactional
    public AssignmentResponse assignEmployee(AssignmentRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.employeeId()));
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.projectId()));

        if (assignmentRepository.existsByEmployeeIdAndProjectId(employee.getId(), project.getId())) {
            throw new DuplicateResourceException(
                    "Employee " + employee.getId() + " is already assigned to project " + project.getId());
        }

        ProjectAssignment assignment = new ProjectAssignment();
        assignment.setEmployee(employee);
        assignment.setProject(project);
        assignment.setRole(request.role().trim());
        assignment.setAssignedDate(LocalDate.now());

        // If two requests pass the check above at the same time, the unique constraint
        // (employee_id, project_id) rejects the second insert. saveAndFlush makes that
        // happen here, and GlobalExceptionHandler turns it into 409 Conflict.
        return AssignmentResponse.from(assignmentRepository.saveAndFlush(assignment));
    }

    @Transactional
    public void removeAssignment(Long id) {
        ProjectAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
        assignmentRepository.delete(assignment);
    }
}
