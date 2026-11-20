package com.example.ems.repository;

import com.example.ems.dto.DepartmentSummary;
import com.example.ems.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /** One aggregate query instead of loading every department and its employees into memory. */
    @Query("""
            SELECT new com.example.ems.dto.DepartmentSummary(d.id, d.name, COUNT(e.id), COALESCE(SUM(e.salary), 0))
            FROM Department d LEFT JOIN d.employees e
            GROUP BY d.id, d.name
            ORDER BY d.name
            """)
    List<DepartmentSummary> getDepartmentSummary();
}
