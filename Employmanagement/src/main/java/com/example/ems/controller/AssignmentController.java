package com.example.ems.controller;

import com.example.ems.dto.AssignmentRequest;
import com.example.ems.dto.AssignmentResponse;
import com.example.ems.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@Tag(name = "Project Assignments", description = "Assign employees to projects. GET needs USER or ADMIN, changes need ADMIN.")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @Operation(summary = "List all project assignments")
    public List<AssignmentResponse> getAssignments() {
        return assignmentService.getAssignments();
    }

    @PostMapping
    @Operation(summary = "Assign an employee to a project")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee assigned"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Employee or project not found"),
            @ApiResponse(responseCode = "409", description = "Employee is already assigned to this project")
    })
    public ResponseEntity<AssignmentResponse> assignEmployee(@Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.assignEmployee(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove an employee from a project")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Assignment removed"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<Void> removeAssignment(@PathVariable Long id) {
        assignmentService.removeAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
