package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Vote;
import jakarta.annotation.Nullable;
import jakarta.persistence.NamedNativeQuery;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    List<Comment> findByContent(String content);

    @Query(nativeQuery = true, value = "SELECT c.id, c.content, c.user_id, c.meeting_id, c.created_at, c.updated_at FROM comment c inner join meeting m on c.meeting_id = m.id WHERE m.platform_link = :link ORDER BY (select count(v) from vote v where v.comment_id = c.id AND v.vote_status = 'UP') " +
            "- (select count(v) from vote v where v.comment_id = c.id AND v.vote_status = 'DOWN') desc")
    List<Comment> findAllByMeeting_PlatformLinkOrderByVoteDesc(@Param("link") String link);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM comment c where c.id in (
                select crc.replied_comments_id from comment_replied_comments crc
                                                                    where crc.comment_id = :commentId
                )
            """)
    List<Comment> findAllCommentsByCommentId(Long commentId);

    @Query(nativeQuery = true, value = "SELECT c.* FROM comment c where c.meeting_id = :meetingId and c.parent_comment_id = -1")
    Page<Comment> findAllPageable(Pageable pageable, Long meetingId);

    @Query(nativeQuery = true, value = "SELECT count(*) from comment_replied_comments where comment_id = :commentId")
    long countByComment_Id(Long commentId);

    List<Comment> findAllByContentLike(String content);

    List<Comment> findAllByParentComment(Comment parentComment);

    Page<Comment> findAllByParentComment_Id(Long parentCommentId, Pageable pageable);

    //@Query("SELECT c from Comment c JOIN c.meeting m WHERE c.meeting.id = m.id AND c.parentComment IS NULL AND m.platformLink = :meetingPlatformLink")
    @Query(value = "SELECT c.* from comment c JOIN meeting m on c.meeting_id = m.id WHERE  c.parent_comment_id IS NULL AND m.platform_link = :meetingPlatformLink", nativeQuery = true)
    Page<Comment> findAllByMeeting_PlatformLink(String meetingPlatformLink, Pageable pageable);

    Long countByParentComment_Id(Long parentCommentId);

   // @NamedNativeQuery()
    //Page<Comment> findAllSpec(Specification<Comment> specification, Pageable pageable);
//    @Query("SELECT c FROM comment c WHERE c.meeting.id = :meetingId AND c.id NOT IN (SELECT rc.repliedComments FROM comment c1 RIGHT JOIN c1.repliedComments rc)")
//    Page<Comment> findAllSpec(Specification<Comment> spec, Pageable pageable, @Param("meetingId") Long meetingId);

    @Query(value = "select count(*) from vote v  where v.voteStatus = 'UP' and v.commentId = :commentId", nativeQuery = true)
    Long findUpVotesCount(Long commentId);

    @Modifying
    @Query(nativeQuery = true, value = "update comment c set up_votes = (select count(*) from vote v  where v.vote_status = 'UP' and v.comment_id = c.id)")
   // @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    void updateCommentUpVoteCount();
//
//    @Override
//    @Query("SELECT c.id, c.content, c.createdAt, c.updatedAt, c.user, c.votes, c.parentComment, c.meeting, c.u from Comment c")
//    Page<Comment> findAll(@Nullable Specification<Comment> specification, Pageable pageable);
}
