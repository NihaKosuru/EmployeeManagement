package com.example.ems.dto;

import com.example.ems.entity.Employee;

import java.math.BigDecimal;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        String designation,
        BigDecimal salary,
        Long departmentId,
        String departmentName,
        Long version) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getDesignation(),
                employee.getSalary(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getVersion());
    }
}
