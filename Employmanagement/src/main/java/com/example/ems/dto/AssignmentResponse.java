package com.example.ems.dto;

import com.example.ems.entity.ProjectAssignment;

import java.time.LocalDate;

public record AssignmentResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long projectId,
        String projectName,
        String role,
        LocalDate assignedDate) {

    public static AssignmentResponse from(ProjectAssignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getEmployee().getId(),
                assignment.getEmployee().getName(),
                assignment.getProject().getId(),
                assignment.getProject().getName(),
                assignment.getRole(),
                assignment.getAssignedDate());
    }
}
