package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Vote;
import com.company.commentsystem.model.enums.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Vote findByComment_Id(Long commentId);
    Set<Vote> findAllByComment_Id (Long commentId);
    Long countAllByComment_IdAndVoteStatus(Long id, VoteStatus voteStatus);
}
