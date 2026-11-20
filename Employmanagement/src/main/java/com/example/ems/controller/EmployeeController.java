package com.example.ems.controller;

import com.example.ems.dto.EmployeeRequest;
import com.example.ems.dto.EmployeeResponse;
import com.example.ems.dto.PageResponse;
import com.example.ems.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Create, read, update, delete and search employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "List employees",
            description = "Optional search by name and filter by department, with pagination and sorting. Role: USER or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of employees"),
            @ApiResponse(responseCode = "400", description = "Invalid page, size or sortBy value"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public PageResponse<EmployeeResponse> getEmployees(
            @Parameter(description = "Part of the employee name (case-insensitive)") @RequestParam(required = false) String name,
            @Parameter(description = "Only employees of this department") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1 to 100") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "One of: id, name, email, designation, salary") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "asc") String direction) {
        return employeeService.getEmployees(name, departmentId, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by id", description = "Role: USER or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public EmployeeResponse getEmployee(@PathVariable Long id) {
        return employeeService.getEmployee(id);
    }

    @PostMapping
    @Operation(summary = "Create an employee", description = "Role: ADMIN. The version field is ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Only ADMIN can create employees"),
            @ApiResponse(responseCode = "404", description = "Department not found"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an employee",
            description = "Role: ADMIN. Send the version you last read. If another user updated the employee "
                    + "after that, the request is rejected with 409 Conflict (optimistic locking).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated, response contains the new version"),
            @ApiResponse(responseCode = "400", description = "Validation failed or version missing"),
            @ApiResponse(responseCode = "404", description = "Employee or department not found"),
            @ApiResponse(responseCode = "409", description = "Stale version or email already exists")
    })
    public EmployeeResponse updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee", description = "Role: ADMIN. Also removes the employee's project assignments.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Employee deleted"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
