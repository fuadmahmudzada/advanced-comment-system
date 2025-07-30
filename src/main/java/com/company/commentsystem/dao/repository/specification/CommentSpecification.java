package com.company.commentsystem.dao.repository.specification;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.model.enums.CommentSearch;
import com.company.commentsystem.service.CommentService;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.text.CollationKey;
import java.util.ArrayList;
import java.util.List;

@Component
public class CommentSpecification {
    private final CommentRepository commentRepository;

    public CommentSpecification(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }
    private List<Long> idList = new ArrayList<>();
    public Specification<Comment> hasContent(String text) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("content"), "%" + text + "%");
    }

    private int count = 0;
    public Specification<Comment> getAllParentComments(CommentSearch commentSearch, Long meetingId) {
        return new Specification<Comment>() {
            /*
                SELECT c.* FROM comment c where c.meeting_id = :meetingId and c.id
                 not in (select replied_comments_id from comment_replied_comments)

                 SELECT * FROM comment c left join comment_replied_comments where crc.commment_id
                 in(
            */
            @Override
            public Predicate toPredicate(Root<Comment> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                CommentSearch firstValue = null;
                if(count!=1){
                     firstValue = commentSearch;
                     count++;
                }
                List<Predicate> predicates = new ArrayList<>();
                Predicate equalToMeetingId = criteriaBuilder.equal(root.get("meeting").get("id"), meetingId);


                if (commentSearch == CommentSearch.SUBCOMMENTS_DEPTH_1) {
                    predicates.add(criteriaBuilder.isNull(root.get("parentComment").get("id")));
                    if(firstValue==CommentSearch.SUBCOMMENTS_DEPTH_1){
                        count = 0;
                    }
                } else if (commentSearch != CommentSearch.SUBCOMMENTS_ALL) {
                    List<Comment> comments = commentRepository.findAll(getAllParentComments(CommentSearch
                            .getInstance(commentSearch.getOrd() - 1), meetingId));

                    idList.addAll(comments.stream().map(x->x.getId()).toList());
                    if(commentSearch.equals(firstValue)){
                        predicates.add(root.get("parentComment").get("id").in(idList));
                        predicates.add(criteriaBuilder.isNull(root.get("parentComment").get("id")));
                        count=0;
                        idList = new ArrayList<>();
                    } else{
                        predicates.add(root.get("parentComment").get("id").in(comments.stream()
                                .map(x -> x.getId()).toList()));
                    }
                }
                if(!predicates.isEmpty()) {
                    Predicate orPredicate = criteriaBuilder.or(predicates.toArray(new Predicate[0]));

                    return criteriaBuilder.and(equalToMeetingId, orPredicate);
                } else{
                    return equalToMeetingId;
                }

                //select c.* from comment c join comment_replied_comments crc on c.id = crc.comment_id where c.id in(c.id + crc.replied_comments_id)
            }
        };

    }


}
