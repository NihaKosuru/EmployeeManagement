package com.example.ems.repository;

import com.example.ems.entity.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    boolean existsByEmployeeIdAndProjectId(Long employeeId, Long projectId);

    @Query("SELECT a FROM ProjectAssignment a JOIN FETCH a.employee JOIN FETCH a.project ORDER BY a.id")
    List<ProjectAssignment> findAllWithEmployeeAndProject();
}
