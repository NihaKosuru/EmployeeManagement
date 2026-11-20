package com.example.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AssignmentRequest(
        @NotNull(message = "Employee id is required")
        @Positive(message = "Employee id must be a positive number")
        Long employeeId,

        @NotNull(message = "Project id is required")
        @Positive(message = "Project id must be a positive number")
        Long projectId,

        @NotBlank(message = "Role is required")
        @Size(max = 100, message = "Role must be at most 100 characters")
        String role) {
}
