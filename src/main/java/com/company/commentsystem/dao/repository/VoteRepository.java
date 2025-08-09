package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Vote;
import com.company.commentsystem.model.enums.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Vote findByComment_Id(Long commentId);
    Set<Vote> findAllByComment_Id (Long commentId);
    Long countAllByComment_IdAndVoteStatus(Long id, VoteStatus voteStatus);

    Long findByComment_IdAndVoteStatus(Long commentId, VoteStatus voteStatus);

    Optional<Vote> findByUser_IdAndComment_Id(Long userId, Long commentId);


    @Modifying
    @Query(value = "DELETE FROM Vote v where v.id = :id")
    void deleteVoteById(Long id);
}
