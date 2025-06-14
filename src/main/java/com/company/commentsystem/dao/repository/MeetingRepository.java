package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
}
