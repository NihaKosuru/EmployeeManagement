package com.example.ems.dto;

import java.math.BigDecimal;

/** Report row filled directly by a JPQL constructor expression (DTO projection). */
public record ProjectSummary(Long projectId, String projectName, BigDecimal budget, Long employeeCount) {
}
