package com.example.ems.repository;

import com.example.ems.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByDepartmentId(Long departmentId);

    /**
     * Search by name and/or department with paging and sorting.
     * JOIN FETCH loads each employee's department in the same SQL query,
     * which avoids the N+1 problem when the department name is mapped to the response.
     */
    @Query(value = """
            SELECT e FROM Employee e JOIN FETCH e.department d
            WHERE (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:departmentId IS NULL OR d.id = :departmentId)
            """,
            countQuery = """
            SELECT COUNT(e) FROM Employee e
            WHERE (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:departmentId IS NULL OR e.department.id = :departmentId)
            """)
    Page<Employee> search(@Param("name") String name, @Param("departmentId") Long departmentId, Pageable pageable);

    @Query("SELECT e FROM Employee e JOIN FETCH e.department d WHERE d.id = :departmentId ORDER BY e.name")
    List<Employee> findByDepartmentWithDepartment(@Param("departmentId") Long departmentId);

    @Query("""
            SELECT e FROM Employee e JOIN FETCH e.department
            WHERE e.id IN (SELECT a.employee.id FROM ProjectAssignment a WHERE a.project.id = :projectId)
            ORDER BY e.name
            """)
    List<Employee> findByProjectWithDepartment(@Param("projectId") Long projectId);
}
