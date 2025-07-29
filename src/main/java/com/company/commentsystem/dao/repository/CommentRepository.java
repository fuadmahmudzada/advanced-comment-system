package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Vote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    List<Comment> findByContent(String content);
    @Query(nativeQuery = true, value = "SELECT c.id, c.content, c.user_id, c.meeting_id, c.created_at, c.updated_at FROM comment c inner join meeting m on c.meeting_id = m.id WHERE m.platform_link = :link ORDER BY (select count(v) from vote v where v.comment_id = c.id AND v.vote_status = 'UP') " +
            "- (select count(v) from vote v where v.comment_id = c.id AND v.vote_status = 'DOWN') desc")
    List<Comment> findAllByMeeting_PlatformLinkOrderByVoteDesc(@Param("link")String link);
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM comment c where c.id in (
                select crc.replied_comments_id from comment_replied_comments crc
                                                                    where crc.comment_id = :commentId
                )
            """)
    List<Comment> findAllCommentsByCommentId(Long commentId);

    @Query(nativeQuery = true, value = "SELECT c.* FROM comment c where c.meeting_id = :meetingId and c.id not in (select replied_comments_id from comment_replied_comments) ")
    Page<Comment> findAllPageable(Pageable pageable, Long meetingId);

    @Query(nativeQuery = true, value = "SELECT count(*) from comment_replied_comments where comment_id = :commentId")
    long countByComment_Id(Long commentId);

//    @Query("SELECT c FROM comment c WHERE c.meeting.id = :meetingId AND c.id NOT IN (SELECT rc.repliedComments FROM comment c1 RIGHT JOIN c1.repliedComments rc)")
//    Page<Comment> findAllSpec(Specification<Comment> spec, Pageable pageable, @Param("meetingId") Long meetingId);

}
