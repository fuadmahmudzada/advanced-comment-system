package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Meeting;
import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.entity.Vote;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.dao.repository.MeetingRepository;
import com.company.commentsystem.dao.repository.UsersRepository;
import com.company.commentsystem.dao.repository.VoteRepository;
import com.company.commentsystem.dao.repository.specification.CommentSpecification;
import com.company.commentsystem.model.dto.*;
import com.company.commentsystem.model.enums.CommentSearch;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.listener.MapPutListener;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

//@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RLocalCachedMap<String, Set<Long>> upVoteMap;
    private final RLocalCachedMap<String, Set<Long>> downVoteMap;
    private final VoteRepository voteRepository;
    private final UsersRepository usersRepository;
    private final MeetingRepository meetingRepository;
    private final CommentSpecification commentSpecification;

    public CommentService(CommentRepository commentRepository, RedisTemplate<String, String> redisTemplate, RLocalCachedMap<String, Set<Long>> upVoteMap, RLocalCachedMap<String, Set<Long>> downVoteMap, VoteRepository voteRepository, UsersRepository usersRepository, MeetingRepository meetingRepository, CommentSpecification commentSpecification) {
        this.commentRepository = commentRepository;
        this.redisTemplate = redisTemplate;
        this.upVoteMap = upVoteMap;
        this.downVoteMap = downVoteMap;
        this.voteRepository = voteRepository;
        this.usersRepository = usersRepository;
        this.meetingRepository = meetingRepository;
        this.commentSpecification = commentSpecification;
    }

//    public void methodWithAdd(){
//        Comment comment = new Comment();
//        comment.setContent("contentcontent1");
//        commentRepository.save(comment);
//        Vote vote = new Vote();
//        vote.setVoteStatus(VoteStatus.UP);
//        comment.addVote(vote);
//        commentRepository.save(comment);
//    }
//    public void addWithoutAdd(){
//        Comment comment = new Comment();
//        comment.setContent("contentcontent1");
//        commentRepository.save(comment);
//    }
//
//    public void voteVithoutAdd(){
//        Vote vote = new Vote();
//        Comment comment =  commentRepository.findByContent("contentcontent1").getFirst();
////        vote.setComment(comment);
//        vote.setVoteStatus(VoteStatus.UP);
//        comment.addVote(vote);

    /// /        List<Vote> votes = comment.getVotes();
    /// /        comment.setVotes(votes);
//        voteRepository.save(vote);
//
//    }
//
//    public void removeMethod(Long id){
//        Comment comment =  commentRepository.findByContent("contentcontent1").getFirst();
//        Vote vote = voteRepository.findById(id).get();
//        comment.removeVote(vote);
//        commentRepository.delete(comment);
//
//    }
    @Transactional
    public CommentDto addComment(CommentAddDto commentAddDto) {
        Comment comment = new Comment();
        Users users = new Users();
        users.setId(commentAddDto.getUserId());
        comment.setContent(commentAddDto.getContent());
        comment.setUser(users);
//        Meeting meeting = new Meeting();
//        meeting.setId(commentAddDto.getMeetingId());
        comment.setMeeting(meetingRepository.findById(commentAddDto.getMeetingId()).get());
        Comment filledComment = commentRepository.save(comment);
        Comment repliedComment = commentRepository.findById(commentAddDto.getRepliedToId()).get();
        //repliedComment.addReply(filledComment);
        CommentDto dto = new CommentDto();
        dto.setContent(filledComment.getContent());
        dto.setId(filledComment.getId());
        dto.setUserId(filledComment.getUser().getId());
        dto.setCreatedAt(filledComment.getCreatedAt());
        return dto;
    }


    public List<CommentDto> getComments(String platformLink) {
        System.out.println(commentRepository.findAllByMeeting_PlatformLinkOrderByVoteDesc(platformLink).getFirst().getUser());
        List<Comment> comments = commentRepository.findAllByMeeting_PlatformLinkOrderByVoteDesc(platformLink);
        List<CommentDto> commentDtos = comments.stream().map(
                comment -> {
                    return new CommentDto(comment.getId(),
                            comment.getContent(),
                            comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                            comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                            comment.getUser().getId(),
                            comment.getCreatedAt(), commentRepository.countByComment_Id(comment.getId()));

                }).toList();
        return commentDtos;
    }

    public CommentDto getCommentById(Long id) {
        Map<Object, Object> cache = redisTemplate.opsForHash().entries("newcommentapp:comment:key:" + id);
        if (!cache.isEmpty()) {
            System.out.println("got from cache");
            CommentDto commentDto = new CommentDto(cache);
            if (upVoteMap.get("newcommentapp:comment:key:upvote:" + id) != null && !upVoteMap.get("newcommentapp:comment:key:upvote:" + id).isEmpty()) {
                commentDto.setUpVote(commentDto.getUpVote() + upVoteMap.get("newcommentapp:comment:key:upvote:" + id).size());
                commentDto.setDownVote(commentDto.getDownVote() + downVoteMap.get("newcommentapp:comment:key:upvote:" + id).size());
            }

            return commentDto;

        }
        Comment comment = commentRepository.findById(id).get();
        CommentDto commentDto = new CommentDto();
        commentDto.setContent(comment.getContent());
        commentDto.setId(comment.getId());
        commentDto.setCreatedAt(comment.getCreatedAt());
        commentDto.setUserId(comment.getUser().getId());
        commentDto.setUpVote((long) comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count());
        commentDto.setDownVote((long) comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count());
        Map<String, CommentDto> map = new HashMap<>();


//        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "id", String.valueOf(commentDto.getId()));
//        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "content", String.valueOf(commentDto.getContent()));
//        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "createdAt", String.valueOf(commentDto.getCreatedAt()));
//        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "userId", String.valueOf(commentDto.getUserId()));
//        upVoteMap.put("newcommentapp:comment:key:upvote:" + id, comment.getVotes().stream().filter(x->x.getVoteStatus()==VoteStatus.UP).map(x->x.getUsers().getId()).collect(Collectors.toSet()));
//        downVoteMap.put("newcommentapp:comment:key:downVote:" + id, comment.getVotes().stream().filter(x->x.getVoteStatus()==VoteStatus.DOWN).map(x->x.getUsers().getId()).collect(Collectors.toSet()));

//        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "upVote", String.valueOf(commentDto.getUpVote()));
//        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "downVote", String.valueOf(commentDto.getDownVote()));
//        redisTemplate.opsForHash().expire("newcommentapp:comment:key:" + id, Duration.ofSeconds(80), List.of("id", "content", "createdAt", "userId", "upVote", "downVote"));
        System.out.println("got from DB");

//        upVoteMap.put("newcommentapp:comment:key:" + "upVote:" + id, voteRepository.findAllByComment_Id(id).stream().map(x -> x.getUsers().getId()).collect(Collectors.toSet()));
//        downVoteMap.put("newcommentapp:comment:key:" + "downVote:" + id, voteRepository.findAllByComment_Id(id).stream().map(x -> x.getUsers().getId()).collect(Collectors.toSet()));
        //     redisTemplate.opsForHash().put("commentapp:comment:key:"+id, "upVote", String.valueOf(comment.getUpVote()));
        //   redisTemplate.opsForHash().put("commentapp:comment:key:" +id, "downVote", String.valueOf(comment.getDownVote()));
        return commentDto;
    }

    //burda baxiriq ki vote edende writebehind sql e salmaliyiq tekce raw sql edilmemelidir
    //set yoxdursa bos biirini yaradiriq ve @Cache ile etmeliyik yuxarini redistemplate
    //ile yox redistemplate ile edende yeni vote lari nece eks etdireceyik
    public void vote(Long id, VoteRequestDto voteRequestDto) {
        Vote vote = new Vote();
        Comment comment = commentRepository.findById(id).get();
        Users users = usersRepository.findById(voteRequestDto.getUserId()).get();
//        Integer upVote = Integer.valueOf((String) redisTemplate.opsForHash().get("commentapp:comment:key:" + id, "upVote"));
        Set<Long> upVoteSet = new HashSet<>();
        Set<Long> downVoteSet = new HashSet<>();

        if (upVoteMap.get("newcommentapp:comment:key:upvote:" + id) != null) {
            upVoteSet = upVoteMap.get("newcommentapp:comment:key:" + "upVote:" + id);
        }
        if (downVoteMap.get("newcommentapp:comment:key:upvote:" + id) != null) {
            downVoteSet = downVoteMap.get("newcommentapp:comment:key:" + "downVote:" + id);
        }
        if (voteRequestDto.getVoteStatus() == VoteStatus.UP) {
            // upVoteSet = upVoteMap.get("newcommentapp:comment:key:" + "upVote:" + id);
            upVoteSet.add(users.getId());
            upVoteMap.putIfAbsent("newcommentapp:comment:key:" + "upVote:" + id, upVoteSet);
        } else {
            //redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", 1);
            downVoteSet.add(users.getId());
            downVoteMap.putIfAbsent("newcommentapp:comment:key:" + "downVote:" + id, downVoteSet);
        }
//        redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", 1);


    }
    //redisTemplate.opsForList();


    public CommentDto getCommentByIdFromDb(Long id) {
        Comment comment = commentRepository.findById(id).get();
        CommentDto commentDto = new CommentDto();
        commentDto.setContent(comment.getContent());
        commentDto.setId(comment.getId());
        commentDto.setCreatedAt(comment.getCreatedAt());
        commentDto.setUserId(comment.getUser().getId());
        commentDto.setUpVote(voteRepository.countAllByComment_IdAndVoteStatus(comment.getId(), VoteStatus.UP));
        commentDto.setDownVote(voteRepository.countAllByComment_IdAndVoteStatus(comment.getId(), VoteStatus.DOWN));
        return commentDto;
    }

    public void voteFromDb(Long id, VoteRequestDto voteRequestDto) {
        Comment comment = commentRepository.findById(id).get();
        Users user = usersRepository.findById(voteRequestDto.getUserId()).get();
        System.out.println(user);
        Optional<Vote> optionalVote = voteRepository.findByUser_IdAndComment_Id(user.getId(), comment.getId());
        if (optionalVote.isPresent()) {
            Vote vote = optionalVote.get();
            if (voteRequestDto.getVoteStatus().equals(vote.getVoteStatus())) {
                voteRepository.deleteById(vote.getId());
            } else {
                vote.setVoteStatus(voteRequestDto.getVoteStatus());
                voteRepository.save(optionalVote.get());
            }
        } else {

            Vote vote = new Vote();
            vote.setVoteStatus(voteRequestDto.getVoteStatus());
            vote.setUser(user);

            comment.addVote(vote);
            user.addVote(vote);

            commentRepository.save(comment);
        }
    }


    public void removeFromDb(Long id) {
        commentRepository.deleteById(id);
    }

    public CommentDto editComment(CommentEditDto commentEditDto) {
        Comment comment = commentRepository.findById(commentEditDto.getId()).get();
        comment.setContent(commentEditDto.getContent());
        commentRepository.save(comment);
        CommentDto commentDto = new CommentDto();
        commentDto.setContent(comment.getContent());
        commentDto.setId(comment.getId());
        commentDto.setCreatedAt(comment.getCreatedAt());
        commentDto.setUserId(comment.getUser().getId());
        commentDto.setUpVote((long) comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count());
        commentDto.setDownVote((long) comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count());
        return commentDto;
    }

    public List<VoteUserDto> getVotes(Long commentId, VoteStatus voteStatus) {
        System.out.println("Vote status in string" + voteStatus);
        System.out.println("Vote status in string" + voteStatus.toString());
        List<Users> usersList = usersRepository.findByCommentIdAndVoteStatus(commentId, voteStatus.toString());
        System.out.println("users list " + usersList.toString());
        return usersList.stream().map(x -> {
            VoteUserDto voteUserDto = new VoteUserDto();
            voteUserDto.setId(x.getId());
            voteUserDto.setFullName(x.getFullName());
            return voteUserDto;
        }).toList();
    }

    public List<CommentDto> getAllCommentsByCommentId(Long commentId) {
        List<Comment> commentList = commentRepository.findAllCommentsByCommentId(commentId);

        Map<Long, List<Comment>> map = commentList.stream().collect(Collectors.groupingBy(x -> x.getParentComment().getId()));

        return commentList.stream().map(comment -> {
            return new CommentDto(comment.getId(),
                    comment.getContent(),
                    comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                    comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                    comment.getUser().getId(),
                    comment.getCreatedAt(), commentRepository.countByComment_Id(comment.getId()));

        }).toList();
    }

    public Page<CommentDto> searchComments(SortType sortType, int pageNumber, int pageSize, Long meetingId, CommentSearchDto commentSearchDto) {
        Sort sort = constructSorting(sortType);

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Specification<Comment> specification = (root, query, cb) -> null;
        if (commentSearchDto.getCommentSearch() != null) {
            specification = specification.and(commentSpecification.getAllParentComments(commentSearchDto.getCommentSearch(), meetingId));
        }

        if (commentSearchDto.getText() != null && !commentSearchDto.getText().isEmpty()) {
            specification = specification.and(commentSpecification.hasContent(commentSearchDto.getText()));
        }

        Page<Comment> resultComments = commentRepository.findAll(specification, pageable);

        if (commentSearchDto.getText() != null && !commentSearchDto.getText().isEmpty()) {
            resultComments.forEach(x -> x.setIsInSearchResult(true));
        }
        List<Comment> modifiableResultComments = new ArrayList<>(resultComments.getContent());
        addParentsToResultList(modifiableResultComments);


        List<CommentDto> lastResultComment = populateReplies(groupComments(modifiableResultComments), null);
        if (lastResultComment == null) {
            /*
            bunu sonradan optional of ile evez elemek lazimdir
             */
            lastResultComment = List.of();
        }

        return new PageImpl<>(lastResultComment, pageable, lastResultComment.size());
    }

    private Map<Long, List<Comment>> groupComments(List<Comment> resultComments) {
        return resultComments.stream().collect(Collectors.toMap(comment -> {
            if (comment.getParentComment() != null) {
                return comment.getParentComment().getId();
            }
            return null;
        }, x -> {
            List<Comment> comments = new ArrayList<>();
            comments.add(x);
            return comments;
        }, (s, a) -> {
            s.addAll(a);
            return s;
        }, HashMap::new));
    }

    private void addParentsToResultList(List<Comment> resultComments) {
        List<Comment> resultCommentParent = new ArrayList<>();
        for (Comment comment : resultComments) {
            while (comment.getParentComment() != null) {
                Comment parentComment = comment.getParentComment();
                if (!resultComments.contains(parentComment) && !resultCommentParent.contains(parentComment)) {
                    resultCommentParent.add(parentComment);
                }
                comment = parentComment;
            }
        }

        resultComments.addAll(resultCommentParent);
    }

    private List<CommentDto> populateReplies(Map<Long, List<Comment>> map, Long parentId) {
        List<Comment> commentsRepliedToParent = map.get(parentId);
        if (commentsRepliedToParent != null) {

            List<CommentDto> commentsRepliedToParentDtos = commentsRepliedToParent.stream().map(comment -> {
                CommentDto commentDto = new CommentDto(comment.getId(),
                        comment.getContent(),
                        comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                        comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                        comment.getUser().getId(),
                        comment.getCreatedAt());

                commentDto.setIsInSearchResult(comment.getIsInSearchResult());
                return commentDto;
            }).toList();
            commentsRepliedToParentDtos.forEach(x -> {
                List<CommentDto> res = populateReplies(map, x.getId());
                x.setReplies(res);
            });

            return commentsRepliedToParentDtos;
        }
        return null;


    }


    private Sort constructSorting(SortType sortType) {
        Sort liked = JpaSort.unsafe("(select count(*) from vote where vote_status = 'UP' and comment_id = c.id) - (select count(*) from vote where vote_status = 'DOWN' and comment_id = c.id)");
        return switch (sortType) {
            case BEST -> liked.descending();
            case NEW -> Sort.by("createdAt").descending();
            case OLD -> Sort.by("createdAt").ascending();
            case LEAST_LIKED -> liked.ascending();
            case TOP ->
                    JpaSort.unsafe("(select count(*) from vote where vote_status = 'UP' and comment_id = c.id)").descending();
            case HOT -> JpaSort.unsafe("""
                     log(abs((select count(*) from vote where vote_status = 'UP' and\s
                     comment_id = c.id) - (select count(*) from vote where vote_status = 'DOWN'
                     and comment_id = c.id))) + (extract(epoch from (now() - c.created_at))/4500)
                    \s""");
        };
    }

}

