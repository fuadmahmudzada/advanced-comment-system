package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.model.enums.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UsersRepository extends JpaRepository<Users, Long> {
    @Query(nativeQuery = true,
            value = "SELECT u.id , u.full_name FROM users u join vote v on u.id = v.user_id join comment c on c.id = v.comment_id WHERE v.comment_id = :commentId and v.vote_status = :voteStatus")
    List<Users> findByCommentIdAndVoteStatus(Long commentId, String voteStatus);

}
