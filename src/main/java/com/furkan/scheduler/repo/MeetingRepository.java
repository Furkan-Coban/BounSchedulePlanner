package com.furkan.scheduler.repo;

import com.furkan.scheduler.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByOffering_Id(Long offeringId);

    void deleteByOffering_Id(Long offeringId);
}
