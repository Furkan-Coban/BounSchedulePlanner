package com.furkan.scheduler.repo;

import com.furkan.scheduler.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public interface TermRepository extends JpaRepository<Term,Long> {

    Optional<Term> findByCode(String code);
}
