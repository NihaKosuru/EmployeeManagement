package com.example.ems.dto;

import com.example.ems.entity.Project;

import java.math.BigDecimal;

public record ProjectResponse(Long id, String name, String description, BigDecimal budget, Long version) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                project.getBudget(), project.getVersion());
    }
}
