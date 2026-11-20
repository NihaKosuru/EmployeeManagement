package com.example.ems.dto;

import java.math.BigDecimal;

/** Report row filled directly by a JPQL constructor expression (DTO projection). */
public record DepartmentSummary(Long departmentId, String departmentName, Long employeeCount, BigDecimal totalSalary) {
}
