package com.furkan.scheduler.repo;

import com.furkan.scheduler.domain.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseOfferingRepository
        extends JpaRepository<CourseOffering, Long> {

    // Used during sync to avoid duplicate offerings
    Optional<CourseOffering> findByTerm_CodeAndDepartment_CodeAndCourseCodeSec(
            String termCode,
            String departmentCode,
            String courseCodeSec
    );

    // Search by course code (CMPE140, PHYS101, etc.)
    List<CourseOffering> findTop200ByTerm_CodeAndCourseCodeSecContainingIgnoreCaseOrderByCourseCodeSecAsc(
            String termCode,
            String query
    );
    List<CourseOffering> findByTerm_CodeOrderByCourseCodeSecAsc(String termCode);
    // Search by course name
    List<CourseOffering> findTop200ByTerm_CodeAndCourseNameContainingIgnoreCaseOrderByCourseCodeSecAsc(
            String termCode,
            String query
    );

    // List all offerings for a department (used for filters)
    List<CourseOffering> findByTerm_CodeAndDepartment_CodeOrderByCourseCodeSecAsc(
            String termCode,
            String departmentCode
    );
}