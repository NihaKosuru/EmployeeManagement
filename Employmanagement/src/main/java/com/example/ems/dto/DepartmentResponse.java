package com.example.ems.dto;

import com.example.ems.entity.Department;

public record DepartmentResponse(Long id, String name, String location, Long version) {

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(),
                department.getLocation(), department.getVersion());
    }
}
