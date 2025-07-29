package com.company.commentsystem.dao.repository.specification;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.model.enums.CommentSearch;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.relational.core.query.Criteria;

import java.util.ArrayList;
import java.util.List;

public class CommentSpecification {
    public Specification<Comment> hasContent(String text) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("content"), text);
    }

    public Specification<Comment> includeReplies(CommentSearch commentSearch, Long meetingId) {
        return new Specification<Comment>() {
            /*
                SELECT c.* FROM comment c where c.meeting_id = :meetingId and c.id
                 not in (select replied_comments_id from comment_replied_comments)

                 SELECT * FROM comment c left join comment_replied_comments where crc.commment_id
                 in(
            */
            @Override
            public Predicate toPredicate(Root<Comment> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//                if(query.getResultType() == Comment.class){
//                    root.fetch("repliedComments", JoinType.LEFT);
//                }
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(criteriaBuilder.equal(root.get("meeting").get("id"), meetingId));
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Comment> subRoot = subquery.from(Comment.class);
                Join<Comment,Comment> joinCommentReplies = subRoot.join("repliedComments");
                subquery.select(joinCommentReplies.get("repliedComments"));


                predicates.add(criteriaBuilder.not(root.get("id").in(subquery)));
                System.out.println("Total predicates created: " + predicates.size());
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
               //select c.* from comment c join comment_replied_comments crc on c.id = crc.comment_id where c.id in(c.id + crc.replied_comments_id)
            }
        };
    }

}
