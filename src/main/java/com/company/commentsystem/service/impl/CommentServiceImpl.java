package com.company.commentsystem.service.impl;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.entity.Vote;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.dao.repository.MeetingRepository;
import com.company.commentsystem.dao.repository.UsersRepository;
import com.company.commentsystem.dao.repository.VoteRepository;
import com.company.commentsystem.dao.repository.specification.CommentSpecification;
import com.company.commentsystem.model.dto.comment_dto.*;
import com.company.commentsystem.model.dto.vote_dto.VoteRequestDto;
import com.company.commentsystem.model.dto.vote_dto.VoteUserDto;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import com.company.commentsystem.model.exception.ResourceNotFoundException;
import com.company.commentsystem.model.mapper.CommentMapper;
import com.company.commentsystem.service.CommentService;
import org.redisson.MapWriterTask;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.RMap;
import org.redisson.api.map.MapWriter;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

//@RequiredArgsConstructor

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RMap<String, Set<Long>> upVoteMap;
    private final RMap<String, Set<Long>> downVoteMap;
    private final RLocalCachedMap<String, Set<Long>> voteMap;
    private final RLocalCachedMap<String, Set<Long>> voteMapOnRemove;
    private final RMap<String, Set<Long>> upVoteMapOnRemove;
    private final RMap<String, Set<Long>> downVoteMapOnRemove;
    private final RMap<String, Set<Long>> downVoteMapOnUpdate;
    private final RMap<String, Set<Long>> upVoteMapOnUpdate;
    private final RList<String> markedForRemoval;
    private final VoteRepository voteRepository;
    private final UsersRepository usersRepository;
    private final MeetingRepository meetingRepository;
    private final CommentSpecification commentSpecification;
    private final MapWriter<String, Set<Long>> voteMapWriter;
    private final RedissonClient redissonClient;
    private final ClientHttpConnector clientHttpConnector;

    public CommentServiceImpl(CommentRepository commentRepository, RedisTemplate<String, String> redisTemplate, RMap<String, Set<Long>> upVoteMap, RMap<String, Set<Long>> downVoteMap, RLocalCachedMap<String, Set<Long>> voteMapOnRemove, RMap<String, Set<Long>> upVoteMapOnRemove, RMap<String, Set<Long>> downVoteMapOnRemove, RMap<String, Set<Long>> downVoteMapOnUpdate, RMap<String, Set<Long>> upVoteMapOnUpdate, VoteRepository voteRepository, UsersRepository usersRepository, MeetingRepository meetingRepository, CommentSpecification commentSpecification, RLocalCachedMap<String, Set<Long>> voteMap, RList<String> markedForRemoval, MapWriter<String, Set<Long>> voteMapWriter, RedissonClient redissonClient, ClientHttpConnector clientHttpConnector) {
        this.commentRepository = commentRepository;
        this.redisTemplate = redisTemplate;
        this.upVoteMap = upVoteMap;
        this.downVoteMap = downVoteMap;
        this.voteMapOnRemove = voteMapOnRemove;
        this.upVoteMapOnRemove = upVoteMapOnRemove;
        this.downVoteMapOnRemove = downVoteMapOnRemove;
        this.downVoteMapOnUpdate = downVoteMapOnUpdate;
        this.upVoteMapOnUpdate = upVoteMapOnUpdate;
        this.markedForRemoval = markedForRemoval;
        this.voteMapWriter = voteMapWriter;
        this.voteMap = voteMap;
        this.voteRepository = voteRepository;
        this.usersRepository = usersRepository;
        this.meetingRepository = meetingRepository;
        this.commentSpecification = commentSpecification;
        this.redissonClient = redissonClient;
        this.clientHttpConnector = clientHttpConnector;
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
        users.addComment(comment);
//        Meeting meeting = new Meeting();
//        meeting.setId(commentCreateDto.getMeetingId());
        comment.setMeeting(meetingRepository.findById(commentCreateDto.getMeetingId()).orElseThrow(() -> new ResourceNotFoundException(String.format("Meeting %d not found", commentCreateDto.getMeetingId()))));
        voteToSelf(comment, users);
        Comment filledComment = commentRepository.save(comment);
        if (commentCreateDto.getRepliedToId() == -1) comment.setParentComment(null);
        else
            comment.setParentComment(commentRepository.findById(commentCreateDto.getRepliedToId()).orElseThrow(() -> new ResourceNotFoundException(String.format("Parent with id %d couldn't be found", commentCreateDto.getRepliedToId()))));
        //repliedComment.addReply(filledComment);
//        CommentCreateResponseDto commentCreateResponseDto = new CommentCreateResponseDto();
//        commentCreateResponseDto.setContent(filledComment.getContent());
//        commentCreateResponseDto.setId(filledComment.getId());
//        commentCreateResponseDto.setUpVotes(filledComment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count());
//        commentCreateResponseDto.setDownVotes(filledComment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count());
        return CommentMapper.INSTANCE.toCommentCreateResponseDto(filledComment);
    }
//proxy method xaricin' oturmek

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
        return comments.map(comment -> CommentMapper.INSTANCE.toCommentResponseDto(comment, commentRepository.countByParentComment_Id(comment.getId())));
//        return comments.map(
//                comment -> {
//                    return new CommentResponseDto(comment.getId(),
//                            comment.getContent(),
//                            comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
//                            comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
//                            commentRepository.countByParentComment_Id(comment.getId()),
//                            comment.getCreatedAt(),
//                            comment.getIsDeleted());
//
//                });
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
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(String.format("Comment %d not found", id)));
//        commentResponseDto.setContent(comment.getContent());
//        commentResponseDto.setId(comment.getId());
//        commentResponseDto.setCreatedAt(comment.getCreatedAt());
//        commentResponseDto.setUpVotes(voteRepository.countAllByComment_IdAndVoteStatus(comment.getId(), VoteStatus.UP));
//        commentResponseDto.setDownVotes(voteRepository.countAllByComment_IdAndVoteStatus(comment.getId(), VoteStatus.DOWN));
//        commentResponseDto.setRepliedCommentCount(commentRepository.countByParentComment_Id(comment.getId()));
        return CommentMapper.INSTANCE.toCommentResponseDto(comment, voteRepository.countAllByComment_IdAndVoteStatus(comment.getId(), VoteStatus.UP), voteRepository.countAllByComment_IdAndVoteStatus(comment.getId(), VoteStatus.DOWN), commentRepository.countByParentComment_Id(comment.getId()));
    }

    @Transactional
    public String voteFromDb(Long commentId, VoteRequestDto voteRequestDto) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException(String.format("Comment %d not found", commentId)));
        ;
        Users votingUser = usersRepository.findById(voteRequestDto.getUserId()).orElseThrow(() -> new ResourceNotFoundException(String.format("User %d not found", voteRequestDto.getUserId())));
        ;
        System.out.println(votingUser);
        Optional<Vote> optionalVote = voteRepository.findByUser_IdAndComment_Id(votingUser.getId(), comment.getId());
        if (optionalVote.isPresent()) {
            Vote vote = optionalVote.get();
            if (voteRequestDto.getVoteStatus().equals(vote.getVoteStatus())) {
                comment.removeVote(vote);
                return "User withdraw %s vote";
            } else {
                vote.setVoteStatus(voteRequestDto.getVoteStatus());
                //commentRepository.save(comment);
                return "User changed to %s vote";
            }
        } else {

            Vote vote = new Vote();
            vote.setVoteStatus(voteRequestDto.getVoteStatus());
            vote.setUser(votingUser);
            System.out.println("before somemethod");

            System.out.println("after somemethod");
            System.out.println("before comment add vote");
            comment.addVote(vote);
            System.out.println("after comment add vote");
            votingUser.addVote(vote);
            System.out.println("after user add vote");

            commentRepository.save(comment);
            return "User voted %s";
        }


    }

    @Transactional(readOnly = true)
    public String voteFromRedis(Long commentId, VoteRequestDto voteRequestDto) {


        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException(String.format("Comment %d not found", commentId)));
        Users votingUser = usersRepository.findById(voteRequestDto.getUserId()).orElseThrow(() -> new ResourceNotFoundException(String.format("User %d not found", voteRequestDto.getUserId())));

        System.out.println(votingUser);
        Optional<Vote> optionalVote = voteRepository.findByUser_IdAndComment_Id(votingUser.getId(), comment.getId());
        Vote optionalVoteCache = new Vote();
        optionalVoteCache.setVoteStatus(voteRequestDto.getVoteStatus());
        optionalVoteCache.setComment(comment);
        optionalVoteCache.setUser(votingUser);
        VoteStatus statusInCache = null;

        boolean isNotInCache = true;
//        if (voteMap.get("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId()) != null) {
//            isNotInCache = !voteMap.get("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId()).contains(votingUser.getId());
//            statusInCache = voteRequestDto.getVoteStatus();
//
//        }
//        short ordinal;
//        if (voteRequestDto.getVoteStatus().ordinal() == 1) {
//            ordinal = 0;
//        } else {
//            ordinal = 1;
//        }
//        if (voteMap.get("last:comment:" + VoteStatus.getByOrdinal((int) ordinal).toString().toLowerCase() + ":" + comment.getId()) != null) {
//            isNotInCache = !voteMap.get("last:comment:" + VoteStatus.getByOrdinal((int) ordinal).toString().toLowerCase() + ":" + comment.getId()).contains(votingUser.getId());
//            if (voteMap.get("last:comment:" + VoteStatus.getByOrdinal((int) ordinal).toString().toLowerCase() + ":" + comment.getId()).contains(votingUser.getId())) {
//                statusInCache = (VoteStatus.getByOrdinal((int) ordinal));
//            }
//        }
        System.out.println("upvote  map " + redissonClient.<String, Set<Long>>getMap("upVoteMap"));
        if (redissonClient.<String, Set<Long>>getMap("upVoteMap") != null) {
            System.out.println("upVoteMap map " + redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up" + comment.getId()));
            System.out.println("upVoteMap map keys " + redissonClient.<String, Set<Long>>getMap("upVoteMap").keySet());
            System.out.println("upVoteMap map values " + redissonClient.<String, Set<Long>>getMap("upVoteMap").values());
        }
        if (redissonClient.<String, Set<Long>>getMap("upVoteMap") != null && redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up:" + comment.getId()) != null) {
            System.out.println("Set: " + redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up:" + comment.getId()));
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up:" + comment.getId()).contains(votingUser.getId());
            statusInCache = VoteStatus.UP;
            System.out.println("Comment id set " + redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up:" + comment.getId()));
        }
//        System.out.println("isnotincache " + redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up" + comment.getId()).contains(votingUser.getId()));
//        System.out.println("UPVOTE MAP red client up" + redissonClient.getMap("last:comment:up" + comment.getId()).keySet());
//        System.out.println("UPVOTE MAP upvotemap" + redissonClient.getMap("upVoteMap").values());
//        System.out.println("DOWNVOTE MAP " + downVoteMap.get("last:comment:up" + comment.getId()));
//        System.out.println("isnotincache " + redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:up" + comment.getId()).contains(votingUser.getId()));
        System.out.println("downVoteMap  map " + redissonClient.<String, Set<Long>>getMap("downVoteMap"));
        if (redissonClient.<String, Set<Long>>getMap("downVoteMap") != null) {
            System.out.println("downVoteMap map keys " + redissonClient.<String, Set<Long>>getMap("downVoteMap").keySet());
            System.out.println("downVoteMap map values " + redissonClient.<String, Set<Long>>getMap("downVoteMap").values());
            System.out.println("downvote map " + redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()));
        }
        if (redissonClient.<String, Set<Long>>getMap("downVoteMap") != null && redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()).contains(votingUser.getId());
            statusInCache = VoteStatus.DOWN;
            System.out.println("Comment id set " + redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()));
        }

        if (optionalVote.isPresent() && redissonClient.<String, Set<Long>>getMap("downVoteMapOnUpdate") != null && redissonClient.<String, Set<Long>>getMap("downVoteMapOnUpdate").get("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("downVoteMapOnUpdate").get("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId()).contains(votingUser.getId());
            statusInCache = VoteStatus.DOWN;
            //System.out.println("Comment id set " + redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()));
        }

        if (optionalVote.isPresent() && redissonClient.<String, Set<Long>>getMap("upVoteMapOnUpdate") != null && redissonClient.<String, Set<Long>>getMap("upVoteMapOnUpdate").get("last:comment:up:" + optionalVote.get().getId() + ":" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("upVoteMapOnUpdate").get("last:comment:up:" + optionalVote.get().getId() + ":" + comment.getId()).contains(votingUser.getId());
            statusInCache = VoteStatus.UP;
        }

        if (optionalVote.isPresent() && redissonClient.<String, Set<Long>>getMap("upVoteMapOnRemove") != null && redissonClient.<String, Set<Long>>getMap("upVoteMapOnRemove").get("last:comment:up:" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("upVoteMapOnRemove").get("last:comment:up:" + comment.getId()).contains(votingUser.getId());
            statusInCache = VoteStatus.UP;
        }

        if (optionalVote.isPresent() && redissonClient.<String, Set<Long>>getMap("downVoteMapOnRemove") != null && redissonClient.<String, Set<Long>>getMap("downVoteMapOnRemove").get("last:comment:down:" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("downVoteMapOnRemove").get("last:comment:down:" + comment.getId()).contains(votingUser.getId());
            statusInCache = VoteStatus.DOWN;
        }
        RList<MapWriterTask.Add> list = redissonClient.getList("{upVoteMap}:write-behind-queue");
        System.out.println("write behind" + list);
        if (list.get(0) != null) {
            System.out.println(list.get(0).getMap());
        }
        System.out.println("Is not in cache" + isNotInCache);
        System.out.println("status in cache" + statusInCache);
        Set<Long> upVoteSet = new HashSet<>();
        Set<Long> downVoteSet = new HashSet<>();
        if (optionalVote.isEmpty()) {
            if (isNotInCache) {
                Vote vote = new Vote();
                vote.setVoteStatus(voteRequestDto.getVoteStatus());
                vote.setUser(votingUser);

                comment.addVote(vote);

                votingUser.addVote(vote);
                if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                    upVoteSet.add(votingUser.getId());
                    upVoteMap.put("last:comment:" + vote.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteSet);

                    upVoteMap.expire(Duration.ofMillis(100000));
                } else {
                    downVoteSet.add(votingUser.getId());
                    boolean isSet = upVoteMap.expire(Duration.ofMillis(100000));
                    System.out.println("ISSET" + isSet);
                    downVoteMap.put("last:comment:" + vote.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteSet);
                    downVoteMap.expire(Duration.ofMillis(100000));
                }
                return String.format("User vote %s saved to cache", vote.getVoteStatus());
            } else {
                //cache de var
                if (upVoteMap.get("last:comment:up" + comment.getId()) != null) {
                    upVoteSet = upVoteMap.get("last:comment:up" + comment.getId());
                }
                if (downVoteMap.get("last:comment:down" + comment.getId()) != null) {
                    downVoteSet = downVoteMap.get("last:comment:down" + comment.getId());
                }
                //cachedeki ve gelen statusu eynidirse
                if (statusInCache == voteRequestDto.getVoteStatus()) {
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        markedForRemoval.add("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId() + votingUser.getId());
                        //write behind elave olunanda cache hamisi silinmelidi
                        // upVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteSet);
                        //upVoteMap.expire(Duration.ofMillis(100000));
                    } else {
                        markedForRemoval.add("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId() + votingUser.getId());
//                        downVoteSet.remove(votingUser.getId());
//                        downVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteSet);
//                        downVoteMap.expire(Duration.ofMillis(100000));
                    }
                    return String.format("User withdrawed vote %s, this change updated to the cache", voteRequestDto.getVoteStatus());
                } else {
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        //downVoteSet.remove(votingUser.getId());
                        markedForRemoval.add("last:comment:DOWN:" + comment.getId() + votingUser.getId());
                        upVoteSet.add(votingUser.getId());
                        upVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteSet);
                        upVoteMap.expire(Duration.ofMillis(100000));
                    } else {
                        //upVoteSet.remove(votingUser.getId());
                        markedForRemoval.add("last:comment:UP:" + comment.getId() + votingUser.getId());
                        downVoteSet.add(votingUser.getId());
                        downVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteSet);
                        downVoteMap.expire(Duration.ofMillis(100000));
                    }// if else etmekdense bele ede bilersen

                    return String.format("User withdrawed vote %s, and voted to other. this change updated to the cache", voteRequestDto.getVoteStatus());
                }
            }
            //vote db da var upvote ve ya downvote
        } else {
            Set<Long> upVoteOnRemoveSet = new HashSet<>();
            Set<Long> downVoteOnRemoveSet = new HashSet<>();
            Set<Long> upVoteOnUpdateSet = new HashSet<>();
            Set<Long> downVoteOnUpdateSet = new HashSet<>();
            if (isNotInCache) {
                //remove olunmalidi bazadan
                if (optionalVote.get().getVoteStatus() == voteRequestDto.getVoteStatus()) {
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        //upVoteOnRemoveSet.add(votingUser.getId());
                        upVoteMap.remove("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId() + ":" + votingUser.getId());
                        //markedForRemoval.add("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId() + ":"+ votingUser.getId());
                        //upVoteMapOnRemove.put("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteOnRemoveSet);
                        //upVoteMapOnRemove.expire(Duration.ofMillis(100000));

                    } else {
                        //downVoteOnRemoveSet.add(votingUser.getId());
                        downVoteMap.remove("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteOnRemoveSet);
                        //downVoteMapOnRemove.put("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteOnRemoveSet);
                        //markedForRemoval.add("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId() + ":"+ votingUser.getId());
                        //downVoteMapOnRemove.expire(Duration.ofMillis(100000));
                    }
                    return String.format("User vote %s is scheduled to remove from DB", voteRequestDto.getVoteStatus());
                } else {
                    //bazadan sil yenisini yaz
                    //bazadakini silmek ucun cach-e elave edirik
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        upVoteOnUpdateSet.add(votingUser.getId());
                        upVoteMapOnUpdate.put("last:comment:up:" + optionalVote.get().getId() + ":" + commentId, upVoteOnUpdateSet);
                        upVoteMapOnUpdate.expire(Duration.ofMillis(100000));
                    } else {
                        downVoteOnUpdateSet.add(votingUser.getId());
                        downVoteMapOnUpdate.put("last:comment:down:" + optionalVote.get().getId() + ":" + commentId, downVoteOnUpdateSet);
                        downVoteMapOnUpdate.expire(Duration.ofMillis(100000));
                    }
//                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
//                        downVoteOnRemoveSet.add(votingUser.getId());
//                        downVoteMapOnRemove.put("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteOnRemoveSet);
//                        downVoteMapOnRemove.expire(Duration.ofMillis(90000));
//
//                    } else {
//                        upVoteOnRemoveSet.add(votingUser.getId());
//                        upVoteMapOnRemove.put("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteOnRemoveSet);
//                        upVoteMapOnRemove.expire(Duration.ofMillis(90000));
//                    }
//                    //bazaya save ucun cache elave edirik
//                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
//                        upVoteSet.add(votingUser.getId());
//
//                        upVoteMap.put("last:comment:up:" + comment.getId(), upVoteSet);
//                        upVoteMap.expire(Duration.ofMillis(100000));
//                    } else {
//
//
//                        downVoteSet.add(votingUser.getId());
//                        downVoteMap.put("last:comment:down:" + comment.getId(), downVoteSet);
//                        downVoteMap.expire(Duration.ofMillis(100000));
//                    }
                    return String.format("User other vote is scheduled to remove from DB and %s is added to cache", voteRequestDto.getVoteStatus());
                }
            } else {
                if (upVoteMap.get("last:comment:up" + comment.getId()) != null) {
                    upVoteSet = upVoteMap.get("last:comment:up" + comment.getId());
                }
                if (downVoteMap.get("last:comment:down" + comment.getId()) != null) {
                    downVoteSet = downVoteMap.get("last:comment:down" + comment.getId());
                }
                if (upVoteMapOnUpdate.get("last:comment:up:" + optionalVote.get().getId() + ":" + comment.getId()) != null) {
                    upVoteOnUpdateSet = upVoteMapOnUpdate.get("last:comment:up:" + optionalVote.get().getId() + ":" + comment.getId());
                }
                if (downVoteMapOnUpdate.get("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId()) != null) {
                    downVoteOnUpdateSet = downVoteMapOnUpdate.get("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId());
                }
                if (downVoteMapOnRemove.get("last:comment:down" + ":" + comment.getId()) != null) {
                    downVoteOnRemoveSet = downVoteMapOnRemove.get("last:comment:down" + ":" + comment.getId());
                }
                if (upVoteMapOnRemove.get("last:comment:up" + ":" + comment.getId()) != null) {
                    upVoteOnRemoveSet = upVoteMapOnRemove.get("last:comment:up" + ":" + comment.getId());
                }
                //cachededi bazadadi
                if (statusInCache == optionalVote.get().getVoteStatus()) {
                    if (statusInCache == VoteStatus.UP) {
                        //upVoteOnRemoveSet.remove(votingUser.getId());
                        markedForRemoval.add("last:comment:up:" + comment.getId() + votingUser.getId());

                        if (voteRequestDto.getVoteStatus() == VoteStatus.DOWN) {
                            //yenisini elave et
                            downVoteOnUpdateSet.add(votingUser.getId());
                            downVoteMapOnUpdate.put("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId(), downVoteOnUpdateSet);
                            downVoteMapOnUpdate.expire(Duration.ofMillis(100000));
                            return "Vote is updated in cache from UP to DOWN";
                        }
                        return "Vote is removed from cache";
                    } else {

                        markedForRemoval.add("last:comment:down:" + comment.getId() + votingUser.getId());

                        if (voteRequestDto.getVoteStatus() == VoteStatus.UP) {
                            //yenisini elave et
                            upVoteOnUpdateSet.add(votingUser.getId());
                            upVoteMapOnUpdate.put("last:comment:up:" + optionalVote.get().getId() + ":" + comment.getId(), downVoteOnUpdateSet);
                            upVoteMapOnUpdate.expire(Duration.ofMillis(100000));
                        }
                        return "Vote is updated in cache from DOWN to UP";
                    }

                } else {
                    if (statusInCache == VoteStatus.DOWN) {
                        markedForRemoval.add("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId() + votingUser.getId());

                    } else {
                        markedForRemoval.add("last:comment:up:" + optionalVote.get().getId() + ":" + comment.getId() + votingUser.getId());

                    }
                    return "Vote is removed from cache";
                }
            }


        }


//            if (voteMap.get("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId()) != null) {
//                if (voteRequestDto.getVoteStatus() == VoteStatus.UP)
//                    upVoteSet = voteMap.get("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId());
//                else
//                    downVoteSet = voteMap.get("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId());
//            }
//            if (upVoteSet.contains(votingUser.getId())) {
//                optionalVoteCache = Optional.ofNullable(votingUser.getId());
//            }


        // System.out.println("comment before flush: " + comment);
        //    commentRepository.flush();
        //System.out.println("comment after flush: " + comment);
//        return "hello";

//axirinci querynin sebebine bir de sorusacagin sual
    }

    @Transactional
    public void removeFromDb(Long commentId) {
        if (commentRepository.countAllByParentComment_Id(commentId) == 0) {
            commentRepository.deleteById(commentId);
        } else {
            Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException(String.format("Comment %d not found", commentId)));
            ;
            comment.setContent(null);
            comment.setIsInSearchResult(null);
            comment.getVotes().removeIf(x -> x.getId() >= 0);
            comment.setVotes(comment.getVotes());
            comment.setIsDeleted(true);
        }


    }

    @Transactional
    public CommentResponseDto editComment(CommentEditDto commentEditDto, Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException(String.format("Comment %d not found", commentId)));
        ;
        comment.setContent(commentEditDto.getContent());
        commentRepository.save(comment);
//        CommentResponseDto commentResponseDto = new CommentResponseDto();
//        commentResponseDto.setContent(comment.getContent());
//        commentResponseDto.setId(comment.getId());
//        commentResponseDto.setCreatedAt(comment.getCreatedAt());
//        commentResponseDto.setRepliedCommentCount(commentRepository.countByParentComment_Id(comment.getId()));
//        commentResponseDto.setUpVotes((long) comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count());
//        commentResponseDto.setDownVotes((long) comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count());
        return CommentMapper.INSTANCE.toCommentResponseDto(comment, commentRepository.countByParentComment_Id(comment.getId()));
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
            voteUserDto.setProfilePicture(x.getProfilePicture());
            return voteUserDto;
        }).toList();
    }


    @Transactional(readOnly = true)
    public Page<CommentSearchResponseDto> searchComments(SortType sortType, int pageNumber, int pageSize, Long
            meetingId, CommentSearchDto commentSearchDto) {

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
        if (modifiableResultComments.isEmpty()) {
            throw new ResourceNotFoundException("Search couldn't find anything");
        }
        addParentsToResultList(modifiableResultComments);


        Optional<List<CommentSearchResponseDto>> searchedCommentInTreeForm = Optional.ofNullable(populateRepliesToParents(groupCommentsByParent(modifiableResultComments), null));


        return new PageImpl<>(searchedCommentInTreeForm.orElseThrow(() -> new ResourceNotFoundException("Search couldn't find anything")), pageable, searchedCommentInTreeForm.get().size());
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
    private List<CommentSearchResponseDto> populateRepliesToParents(Map<Long, List<Comment>> map, Long
            parentId) {
        List<Comment> commentsRepliedToParent = map.get(parentId);
        if (commentsRepliedToParent != null) {

            List<CommentSearchResponseDto> commentsRepliedToParentDtos = commentsRepliedToParent.stream().map(comment -> {
                CommentSearchResponseDto commentSearchResponseDto = new CommentSearchResponseDto(comment.getId(),
                        comment.getContent(),
                        comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                        comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                        comment.getCreatedAt(),
                        comment.getIsDeleted());

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
// if (statusInCache != optionalVote.get().getVoteStatus()) {
//        if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
//        upVoteSet.remove(votingUser.getId());
//        upVoteOnUpdateSet.remove(votingUser.getId());
//
//        upVoteMapOnUpdate.replace("last:comment:up:" + optionalVote.get().getId() + ":" + comment.getId(), upVoteOnUpdateSet);
//
//        //upVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteSet);
//        //upVoteMap.expire(Duration.ofMillis(100000));
//        upVoteMapOnUpdate.expire(Duration.ofMillis(100000));
//        } else {
//        downVoteSet.remove(votingUser.getId());
//        downVoteOnUpdateSet.remove(votingUser.getId());
//
//
//        downVoteMapOnUpdate.replace("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId(), downVoteOnUpdateSet);
//
//        //downVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteSet);
//        //downVoteMap.expire(Duration.ofMillis(100000));
//        downVoteMapOnUpdate.expire(Duration.ofMillis(100000));
//        }
//        return String.format("Vote %s is removed from cache", voteRequestDto.getVoteStatus());
//        } else {
//        if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
//        upVoteOnRemoveSet.remove(votingUser.getId());
//        if (upVoteOnRemoveSet.isEmpty()) {
//        upVoteMapOnRemove.remove("last:comment:up:" + comment.getId());
//        } else {
//        upVoteMapOnRemove.replace("last:comment:up" + ":" + comment.getId(), upVoteOnRemoveSet);
//        }
/// /                        downVoteSet.remove(votingUser.getId());
/// /                        upVoteSet.add(votingUser.getId());
//
//        upVoteMapOnRemove.expire(Duration.ofMillis(100000));
//        } else {
//        downVoteOnRemoveSet.remove(votingUser.getId());
//        downVoteOnUpdateSet.add(votingUser.getId());
//        //upVoteSet.remove(votingUser.getId());
//        //downVoteSet.add(votingUser.getId());
//        downVoteMapOnRemove.replace("last:comment:down" + ":" + comment.getId(), downVoteOnRemoveSet);
//        downVoteMapOnRemove.expire(Duration.ofMillis(100000));
//        }
//
//        // upVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteSet);
//        // downVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteSet);
//        //upVoteMap.expire(Duration.ofMillis(100000));
//        //downVoteMap.expire(Duration.ofMillis(100000));
//        return String.format("Vote is removed from cache and %s is voted", voteRequestDto.getVoteStatus());
//        }


//                        RList<MapWriterTask.Add> queue = redissonClient.getList("{downVoteMapOnRemove}:write-behind-queue");
//                        boolean remove = false;
//                        int i;
//                        for (i = 0; i < queue.size(); i++) {
//                            if (queue.get(i).getMap().get("last:comment:down:" + comment.getId()) == votingUser.getId()) {
//                                remove = true;
//                            }
//                        }
//                        if (remove) {
//                            queue.remove(i);
//                        }