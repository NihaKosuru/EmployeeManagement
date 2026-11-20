package com.example.ems.service;

import com.example.ems.dto.DepartmentSummary;
import com.example.ems.dto.ProjectSummary;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;

    public ReportService(DepartmentRepository departmentRepository, ProjectRepository projectRepository) {
        this.departmentRepository = departmentRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentSummary> getDepartmentSummary() {
        return departmentRepository.getDepartmentSummary();
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> getProjectSummary() {
        return projectRepository.getProjectSummary();
    }
}
