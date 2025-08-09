package com.company.commentsystem.dao.repository.specification;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Vote;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.model.enums.CommentSearchDeepness;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import jakarta.persistence.criteria.*;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.sqm.TemporalUnit;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CommentSpecification {
    private final CommentRepository commentRepository;

    public CommentSpecification(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    //burda en sondaki id leri saxlayiriq sonra burdaki idlere gore
    //parent i bunlar olan commentleri tapiriq bu da depth deyende cemin nece addim duseceyini gosterir
    //1 depth post commentleridir yeni. Butun sondaki parent idlere gore commentleri tapir
    private List<Long> finalParentCommentIdList = new ArrayList<>();

    public Specification<Comment> hasContent(String text) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("content"), "%" + text + "%");
    }

    //
    public Order orderBy(Root<Comment> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder, SortType sortType) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Subquery<Long> downdVotesSubquery = query.subquery(Long.class);
        Root<Vote> voteRoot = subquery.from(Vote.class);
        Root<Vote> downVoteSubqueryRoot = downdVotesSubquery.from(Vote.class);
        Expression<Long> liked = criteriaBuilder.diff(subquery.select(criteriaBuilder.count(voteRoot)).where(criteriaBuilder.and(criteriaBuilder.equal(voteRoot.get("voteStatus"), VoteStatus.UP),
                criteriaBuilder.equal(voteRoot.get("comment").get("id"), root.get("id")))), downdVotesSubquery.select(criteriaBuilder.count(downVoteSubqueryRoot)).where(criteriaBuilder.and(criteriaBuilder.equal(downVoteSubqueryRoot.get("voteStatus"), VoteStatus.DOWN),
                criteriaBuilder.equal(downVoteSubqueryRoot.get("comment").get("id"), root.get("id")))));

        return switch (sortType) {
            case TOP ->
                    criteriaBuilder.desc(subquery.select(criteriaBuilder.count(voteRoot)).where(criteriaBuilder.and(criteriaBuilder.equal(voteRoot.get("voteStatus"), "UP"),
                            criteriaBuilder.equal(voteRoot.get("comment").get("id"), root.get("id")))));
            case BEST -> criteriaBuilder.desc(liked);
            case HOT -> {
                HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) criteriaBuilder;
                Expression<Long> secondsPassed = hcb.durationByUnit(TemporalUnit.SECOND, hcb.durationBetween(hcb.currentInstant(), root.get("createdAt")));

                Long a = 11l;
                Double b = (double) a;
                System.out.println("SECONDSPASSED" + secondsPassed);
                System.out.println("ABS" + criteriaBuilder.abs(liked));
//                CriteriaBuilder.Case<Vote> voteCase = criteriaBuilder.selectCase();
//                Expression<Long> expression = criteriaBuilder.abs(liked);
//                Expression<Double> log = hcb.log(10, criteriaBuilder.abs(liked));
//                Long abc = 1L;
//
//                Expression<Long> when = criteriaBuilder.selectCase()
//                        .when(criteriaBuilder.notEqual(criteriaBuilder.abs(liked), 0).as(Boolean.class),
//                                1L).otherwise(1L);
//
//                Stream<CharSequence> stream = Stream.of("a", (CharSequence) "b").distinct();
//
//                List<Users> list = Arrays.asList(new Users(), new Users());
//                list.stream().sorted(Comparator.comparing(u->u.getFullName()).reversed());
//                list.sort(Comparator.comparing(u->u.getFullName()).reversed());
//                list.stream().filter(x->x.getId()==1).map(x->x.getFullName()).findFirst();
//
//                Comparator<Users> comparator = Comparator.comparing(u->u.getFullName()).reversed();
//
//
//                Expression<Number> test = criteriaBuilder.selectCase()
//                        .when(criteriaBuilder.notEqual(criteriaBuilder.abs(liked), 0),
//                                hcb.log(10, criteriaBuilder.abs(liked)) )
//                        .otherwise(criteriaBuilder.abs(liked));
//                CriteriaBuilder.Case<Number> caseLog = criteriaBuilder.selectCase();
//                caseLog = caseLog.when(criteriaBuilder.notEqual(criteriaBuilder.abs(liked), 0),
//                        hcb.log(10, criteriaBuilder.abs(liked)));
                //caseLog.otherwise(criteriaBuilder.abs(liked))

                Expression<Number> logExpression = criteriaBuilder.<Number>selectCase().when(criteriaBuilder.notEqual(criteriaBuilder.abs(liked), 0),
                        hcb.log(10, criteriaBuilder.abs(liked))).otherwise(criteriaBuilder.abs(liked));
                yield criteriaBuilder.desc(criteriaBuilder.sum(logExpression, criteriaBuilder.quot(secondsPassed, 4500)));
            }
            case NEW -> criteriaBuilder.desc(root.get("createdAt"));
            case OLD -> criteriaBuilder.asc(root.get("createdAt"));
            case LEAST_LIKED -> criteriaBuilder.asc(liked);
        };


    }

    public Specification<Comment> hasParent(String meetingPlatformLink, Long parentCommentId, SortType sortType) {
        return (root, query, criteriaBuilder) -> {

            Predicate parentCommentPredicate;
            if (parentCommentId == null) {
                parentCommentPredicate = criteriaBuilder.isNull(root.get("parentComment").get("id"));
            } else {
                parentCommentPredicate = criteriaBuilder.equal(root.get("parentComment").get("id"), parentCommentId);
            }
            query.orderBy(orderBy(root, query, criteriaBuilder, sortType));
            return criteriaBuilder.and(criteriaBuilder.equal(root.get("meeting").get("platformLink"), meetingPlatformLink), parentCommentPredicate
            );
        };
    }

    private int count = 0;

    //    Specification<Comment> getAllComments(CommentSearchDeepness commentSearchDeepness, Long meetingId){
//        return new Specification<Comment>() {
//            @Override
//            public Predicate toPredicate(Root<Comment> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//                Predicate equalToMeetingId = criteriaBuilder.equal(root.get("meeting").get("id"), meetingId);
//            }
//        }
//    }
//
    public Specification<Comment> containsCommentsInDefinedDepth(CommentSearchDeepness commentSearchDeepness, Long meetingId, SortType sortType) {
        return new Specification<Comment>() {
            /*
                SELECT c.* FROM comment c where c.meeting_id = :meetingId and c.id
                 not in (select replied_comments_id from comment_replied_comments)

                 SELECT * FROM comment c left join comment_replied_comments where crc.commment_id
                 in(
            */
            @Override
            public Predicate toPredicate(Root<Comment> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                CommentSearchDeepness firstValue = null;
                if (count != 1) {
                    firstValue = commentSearchDeepness;
                    count++;
                }
                List<Predicate> predicates = new ArrayList<>();
                Predicate equalToMeetingId = criteriaBuilder.equal(root.get("meeting").get("id"), meetingId);


//                Subquery<Long> subquery = query.subquery(Long.class);
//                Root<Vote> voteRoot = subquery.from(Vote.class);
//                subquery.select(criteriaBuilder.count(voteRoot)).where(criteriaBuilder.and(criteriaBuilder.equal(voteRoot.get("voteStatus"), "UP"),
//                        criteriaBuilder.equal(voteRoot.get("comment").get("id"), root.get("id"))));
//
//                query.orderBy(criteriaBuilder.asc(subquery));
                query.orderBy(orderBy(root, query, criteriaBuilder, sortType));
                if (commentSearchDeepness == CommentSearchDeepness.SUBCOMMENTS_DEPTH_1) {
                    predicates.add(criteriaBuilder.isNull(root.get("parentComment").get("id")));
                    if (firstValue == CommentSearchDeepness.SUBCOMMENTS_DEPTH_1) {
                        count = 0;
                    }
                } else if (commentSearchDeepness != CommentSearchDeepness.SUBCOMMENTS_ALL) {
                    List<Comment> childComments = commentRepository.findAll(containsCommentsInDefinedDepth(CommentSearchDeepness
                            .getInstance(commentSearchDeepness.getOrd() - 1), meetingId, sortType));

                    finalParentCommentIdList.addAll(childComments.stream().map(x -> x.getId()).toList());
                    if (commentSearchDeepness.equals(firstValue)) {
                        System.out.println("final parent comment "+finalParentCommentIdList);
                        //derinliye uygun gelen commentleri tap
                        predicates.add(root.get("parentComment").get("id").in(finalParentCommentIdList));
                        predicates.add(criteriaBuilder.isNull(root.get("parentComment").get("id")));
                        count = 0;
                        finalParentCommentIdList = new ArrayList<>();
                    } else {
                        predicates.add(root.get("parentComment").get("id").in(childComments.stream()
                                .map(x -> x.getId()).toList()));
                    }
                }
                if (!predicates.isEmpty()) {
                    Predicate orPredicate = criteriaBuilder.or(predicates.toArray(new Predicate[0]));

                    return criteriaBuilder.and(equalToMeetingId, orPredicate);
                } else {
                    return equalToMeetingId;
                }

                //select c.* from comment c join comment_replied_comments crc on c.id = crc.comment_id where c.id in(c.id + crc.replied_comments_id)
            }
        };

    }


}
