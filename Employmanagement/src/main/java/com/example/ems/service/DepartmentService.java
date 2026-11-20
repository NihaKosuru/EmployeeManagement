package com.example.ems.service;

import com.example.ems.dto.DepartmentRequest;
import com.example.ems.dto.DepartmentResponse;
import com.example.ems.dto.EmployeeResponse;
import com.example.ems.entity.Department;
import com.example.ems.exception.DuplicateResourceException;
import com.example.ems.exception.ResourceNotFoundException;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.EmployeeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAll(Sort.by("name")).stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        return DepartmentResponse.from(findDepartment(id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getDepartmentEmployees(Long id) {
        findDepartment(id);
        return employeeRepository.findByDepartmentWithDepartment(id).stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name().trim())) {
            throw new DuplicateResourceException("Department " + request.name() + " already exists");
        }
        Department department = new Department();
        department.setName(request.name().trim());
        department.setLocation(request.location());
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findDepartment(id);
        if (departmentRepository.existsByNameAndIdNot(request.name().trim(), id)) {
            throw new DuplicateResourceException("Department " + request.name() + " already exists");
        }
        department.setName(request.name().trim());
        department.setLocation(request.location());
        departmentRepository.saveAndFlush(department);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = findDepartment(id);
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new IllegalArgumentException("Department " + id + " still has employees. Move or delete them first.");
        }
        departmentRepository.delete(department);
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }
}
