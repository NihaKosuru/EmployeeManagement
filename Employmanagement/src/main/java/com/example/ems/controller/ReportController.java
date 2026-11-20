package com.example.ems.controller;

import com.example.ems.dto.DepartmentSummary;
import com.example.ems.dto.ProjectSummary;
import com.example.ems.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Summary reports. Role: USER or ADMIN.")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/department-summary")
    @Operation(summary = "Employee count and total salary per department")
    public List<DepartmentSummary> getDepartmentSummary() {
        return reportService.getDepartmentSummary();
    }

    @GetMapping("/project-summary")
    @Operation(summary = "Budget and number of assigned employees per project")
    public List<ProjectSummary> getProjectSummary() {
        return reportService.getProjectSummary();
    }
}
