package com.furkan.scheduler.repo;

import com.furkan.scheduler.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
    Optional<Department> findByCode(String code);
}
