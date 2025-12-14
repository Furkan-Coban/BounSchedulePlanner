package com.furkan.scheduler.service;

import com.furkan.scheduler.domain.Department;
import com.furkan.scheduler.domain.Term;
import com.furkan.scheduler.repo.DepartmentRepository;
import com.furkan.scheduler.repo.TermRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class SyncService {

    private final TermRepository termRepo;
    private final DepartmentRepository deptRepo;

    public SyncService(TermRepository termRepo, DepartmentRepository deptRepo) {
        this.termRepo = termRepo;
        this.deptRepo = deptRepo;
    }

    public Map<String, Object> syncDepartment(String termCode, String deptCode, String deptName) {
        Term term = termRepo.findByCode(termCode)
                .orElseGet(() -> termRepo.save(new Term(null, termCode)));

        Department dept = deptRepo.findByCode(deptCode)
                .orElseGet(() -> deptRepo.save(new Department(null, deptCode, deptName)));

        // next step: fetch+parse+upsert offerings
        Map<String, Object> res = new HashMap<>();
        res.put("term", term.getCode());
        res.put("department", dept.getCode());
        res.put("status", "TERM+DEPT READY (fetch/parse not implemented yet)");
        res.put("ts", Instant.now().toString());
        return res;
    }
}
