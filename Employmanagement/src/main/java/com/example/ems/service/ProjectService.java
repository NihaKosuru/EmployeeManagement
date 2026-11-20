package com.example.ems.service;

import com.example.ems.dto.EmployeeResponse;
import com.example.ems.dto.ProjectRequest;
import com.example.ems.dto.ProjectResponse;
import com.example.ems.entity.Project;
import com.example.ems.exception.DuplicateResourceException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.EmployeeRepository;
import com.example.ems.repository.ProjectRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    public ProjectService(ProjectRepository projectRepository, EmployeeRepository employeeRepository) {
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        return projectRepository.findAll(Sort.by("name")).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long id) {
        return ProjectResponse.from(findProject(id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getProjectEmployees(Long id) {
        findProject(id);
        return employeeRepository.findByProjectWithDepartment(id).stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        if (projectRepository.existsByName(request.name().trim())) {
            throw new DuplicateResourceException("Project " + request.name() + " already exists");
        }
        Project project = new Project();
        copyRequestToProject(request, project);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProject(id);
        if (projectRepository.existsByNameAndIdNot(request.name().trim(), id)) {
            throw new DuplicateResourceException("Project " + request.name() + " already exists");
        }
        copyRequestToProject(request, project);
        projectRepository.saveAndFlush(project);
        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        projectRepository.delete(findProject(id));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private void copyRequestToProject(ProjectRequest request, Project project) {
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setBudget(request.budget());
    }
}
