package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.entity.Vote;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.dao.repository.MeetingRepository;
import com.company.commentsystem.dao.repository.UsersRepository;
import com.company.commentsystem.dao.repository.VoteRepository;
import com.company.commentsystem.dao.repository.specification.CommentSpecification;
import com.company.commentsystem.model.dto.*;
import com.company.commentsystem.model.dto.comment_dto.*;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import org.redisson.api.RLocalCachedMap;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public CommentCreateResponseDto addComment(CommentCreateDto commentCreateDto) {
        Comment comment = new Comment();
        Users users = new Users();
        users.setId(commentCreateDto.getUserId());
        comment.setContent(commentCreateDto.getContent());
        comment.setUser(users);
//        Meeting meeting = new Meeting();
//        meeting.setId(commentCreateDto.getMeetingId());
        comment.setMeeting(meetingRepository.findById(commentCreateDto.getMeetingId()).get());
        voteToSelf(comment, users);
        Comment filledComment = commentRepository.save(comment);
        if (commentCreateDto.getRepliedToId() == -1) comment.setParentComment(null);
        else comment.setParentComment(commentRepository.findById(commentCreateDto.getRepliedToId()).get());
        //repliedComment.addReply(filledComment);
        CommentCreateResponseDto commentCreateResponseDto = new CommentCreateResponseDto();
        commentCreateResponseDto.setContent(filledComment.getContent());
        commentCreateResponseDto.setId(filledComment.getId());
        commentCreateResponseDto.setUpVotes(filledComment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count());
        commentCreateResponseDto.setDownVotes(filledComment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count());
        return commentCreateResponseDto;
    }

    private void voteToSelf(Comment comment, Users users) {
        Vote selfVote = new Vote();
        selfVote.setComment(comment);
        selfVote.setUser(users);
        selfVote.setVoteStatus(VoteStatus.UP);
        comment.addVote(selfVote);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getComments(String platformLink, Long parentId, SortType sortType, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize/*, constructSorting(sortType)*/);
        Page<Comment> comments;
        Specification<Comment> specification = ((root, query, criteriaBuilder) -> null);

        if (parentId == -1) {
            specification = specification.and(commentSpecification.hasParent(platformLink, null, sortType));
            comments = commentRepository.findAll(specification, pageable);
            System.out.println(comments.getContent());
        } else {
            specification = specification.and(commentSpecification.hasParent(platformLink, parentId, sortType));
            comments = commentRepository.findAll(specification, pageable);
        }

        return comments.map(
                comment -> {
                    return new CommentResponseDto(comment.getId(),
                            comment.getContent(),
                            comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                            comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                            commentRepository.countByParentComment_Id(comment.getId()),
                            comment.getCreatedAt());

                });
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


    @Transactional(readOnly = true)
    public CommentResponseDto getCommentByIdFromDb(Long id) {
        Comment comment = commentRepository.findById(id).get();
        CommentResponseDto commentResponseDto = new CommentResponseDto();
        commentResponseDto.setContent(comment.getContent());
        commentResponseDto.setId(comment.getId());
        commentResponseDto.setCreatedAt(comment.getCreatedAt());
        commentResponseDto.setUpVotes(voteRepository.countAllByComment_IdAndVoteStatus(comment.getId(), VoteStatus.UP));
        commentResponseDto.setDownVotes(voteRepository.countAllByComment_IdAndVoteStatus(comment.getId(), VoteStatus.DOWN));
        commentResponseDto.setRepliedCommentCount(commentRepository.countByParentComment_Id(comment.getId()));
        return commentResponseDto;
    }

    @Transactional
    public CommentResponseDto voteFromDb(Long commentId, VoteRequestDto voteRequestDto) {
        Comment comment = commentRepository.findById(commentId).get();
        Users votingUser = usersRepository.findById(voteRequestDto.getUserId()).get();
        System.out.println(votingUser);
        Optional<Vote> optionalVote = voteRepository.findByUser_IdAndComment_Id(votingUser.getId(), comment.getId());
        if (optionalVote.isPresent()) {
            Vote vote = optionalVote.get();
            if (voteRequestDto.getVoteStatus().equals(vote.getVoteStatus())) {
                //comment.removeVote(vote);
                voteRepository.deleteById(vote.getId());

            } else {
                vote.setVoteStatus(voteRequestDto.getVoteStatus());
                voteRepository.save(optionalVote.get());
            }
        } else {

            Vote vote = new Vote();
            vote.setVoteStatus(voteRequestDto.getVoteStatus());
            vote.setUser(votingUser);

            comment.addVote(vote);
            votingUser.addVote(vote);

            commentRepository.save(comment);
        }

       // System.out.println("comment before flush: " + comment);
    //    commentRepository.flush();
        //System.out.println("comment after flush: " + comment);
        return new CommentResponseDto(comment.getId(),
                comment.getContent(),
                comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                commentRepository.countByParentComment_Id(comment.getId()),
                comment.getCreatedAt());
    }

    @Transactional
    public void removeFromDb(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public CommentResponseDto editComment(CommentEditDto commentEditDto, Long commentId) {
        Comment comment = commentRepository.findById(commentId).get();
        comment.setContent(commentEditDto.getContent());
        commentRepository.save(comment);
        CommentResponseDto commentResponseDto = new CommentResponseDto();
        commentResponseDto.setContent(comment.getContent());
        commentResponseDto.setId(comment.getId());
        commentResponseDto.setCreatedAt(comment.getCreatedAt());
        commentResponseDto.setRepliedCommentCount(commentRepository.countByParentComment_Id(comment.getId()));
        commentResponseDto.setUpVotes((long) comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count());
        commentResponseDto.setDownVotes((long) comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count());
        return commentResponseDto;
    }

    @Transactional(readOnly = true)
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


    @Transactional(readOnly = true)
    public Page<CommentSearchResponseDto> searchComments(SortType sortType, int pageNumber, int pageSize, Long meetingId, CommentSearchDto commentSearchDto) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Specification<Comment> specification = (root, query, cb) -> null;
        if (commentSearchDto.getCommentSearchDeepness() != null) {
            specification = specification.and(commentSpecification.containsCommentsInDefinedDepth(commentSearchDto.getCommentSearchDeepness(), meetingId, sortType));
        }

        if (commentSearchDto.getText() != null && !commentSearchDto.getText().isEmpty()) {
            specification = specification.and(commentSpecification.hasContent(commentSearchDto.getText()));
        }

        Page<Comment> resultComments = commentRepository.findAll(specification, pageable);

        System.out.println("res com " + resultComments.getContent());
        if (commentSearchDto.getText() != null && !commentSearchDto.getText().isEmpty()) {
            resultComments.forEach(x -> x.setIsInSearchResult(true));
        }
        List<Comment> modifiableResultComments = new ArrayList<>(resultComments.getContent());
        addParentsToResultList(modifiableResultComments);


        List<CommentSearchResponseDto> searchedCommentInTreeForm = populateRepliesToParents(groupCommentsByParent(modifiableResultComments), null);
        if (searchedCommentInTreeForm == null) {
            /*
            bunu sonradan optional of ile evez elemek lazimdir
             */
            searchedCommentInTreeForm = List.of();
        }

        return new PageImpl<>(searchedCommentInTreeForm, pageable, searchedCommentInTreeForm.size());
    }


    private Map<Long, List<Comment>> groupCommentsByParent(List<Comment> resultComments) {
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

    //verieln texti contain eden commentler iyerarxik olaraq gosterilmelidir
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

    //subtree formasinda replylari qur
    private List<CommentSearchResponseDto> populateRepliesToParents(Map<Long, List<Comment>> map, Long parentId) {
        List<Comment> commentsRepliedToParent = map.get(parentId);
        if (commentsRepliedToParent != null) {

            List<CommentSearchResponseDto> commentsRepliedToParentDtos = commentsRepliedToParent.stream().map(comment -> {
                CommentSearchResponseDto commentSearchResponseDto = new CommentSearchResponseDto(comment.getId(),
                        comment.getContent(),
                        comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                        comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                        comment.getCreatedAt());

                commentSearchResponseDto.setIsInSearchResult(comment.getIsInSearchResult());
                return commentSearchResponseDto;
            }).toList();
            commentsRepliedToParentDtos.forEach(x -> {
                List<CommentSearchResponseDto> res = populateRepliesToParents(map, x.getId());
                x.setReplies(res);
            });

            return commentsRepliedToParentDtos;
        }
        return null;


    }

}

