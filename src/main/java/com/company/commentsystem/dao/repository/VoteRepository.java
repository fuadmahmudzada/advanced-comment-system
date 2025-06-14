package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {
}
