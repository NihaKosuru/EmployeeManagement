package com.example.ems.repository;

import com.example.ems.dto.ProjectSummary;
import com.example.ems.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("""
            SELECT new com.example.ems.dto.ProjectSummary(p.id, p.name, p.budget, COUNT(a.id))
            FROM Project p LEFT JOIN p.assignments a
            GROUP BY p.id, p.name, p.budget
            ORDER BY p.name
            """)
    List<ProjectSummary> getProjectSummary();
}
