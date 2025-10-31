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
import org.apache.catalina.User;
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
    private final RSet<String> markedForRemoval;
    private final VoteRepository voteRepository;
    private final UsersRepository usersRepository;
    private final MeetingRepository meetingRepository;
    private final CommentSpecification commentSpecification;
    private final MapWriter<String, Set<Long>> voteMapWriter;
    private final RedissonClient redissonClient;
    private final ClientHttpConnector clientHttpConnector;

    public CommentServiceImpl(CommentRepository commentRepository, RedisTemplate<String, String> redisTemplate, RMap<String, Set<Long>> upVoteMap, RMap<String, Set<Long>> downVoteMap, RLocalCachedMap<String, Set<Long>> voteMapOnRemove, RMap<String, Set<Long>> upVoteMapOnRemove, RMap<String, Set<Long>> downVoteMapOnRemove, RMap<String, Set<Long>> downVoteMapOnUpdate, RMap<String, Set<Long>> upVoteMapOnUpdate, VoteRepository voteRepository, UsersRepository usersRepository, MeetingRepository meetingRepository, CommentSpecification commentSpecification, RLocalCachedMap<String, Set<Long>> voteMap, RSet<String> markedForRemoval, MapWriter<String, Set<Long>> voteMapWriter, RedissonClient redissonClient, ClientHttpConnector clientHttpConnector) {
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

    @Transactional
    public CommentCreateResponseDto addComment(CommentCreateDto commentCreateDto) {
        Comment comment = new Comment();
        Users users = new Users();
        users.setId(commentCreateDto.getUserId());
        comment.setContent(commentCreateDto.getContent());
        comment.setUser(users);
        users.addComment(comment);

        comment.setMeeting(meetingRepository.findById(commentCreateDto.getMeetingId()).orElseThrow(() -> new ResourceNotFoundException(String.format("Meeting %d not found", commentCreateDto.getMeetingId()))));
        voteToSelf(comment, users);
        Comment filledComment = commentRepository.save(comment);
        if (commentCreateDto.getRepliedToId() == -1) comment.setParentComment(null);
        else
            comment.setParentComment(commentRepository.findById(commentCreateDto.getRepliedToId()).orElseThrow(() -> new ResourceNotFoundException(String.format("Parent with id %d couldn't be found", commentCreateDto.getRepliedToId()))));

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

    //redisTemplate.opsForList();


    @Transactional(readOnly = true)
    public CommentResponseDto getCommentByIdFromDb(Long id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(String.format("Comment %d not found", id)));

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

        VoteStatus statusInCache = null;
        Boolean isNotInCache = true;
        statusInCache = initializeCacheVoteStatus(statusInCache, optionalVote, comment, votingUser);
        isNotInCache = initializeCacheStatus(isNotInCache, optionalVote, comment, votingUser);
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
                    downVoteMap.put("last:comment:" + vote.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteSet);
                    downVoteMap.expire(Duration.ofMillis(100000));
                }
                return String.format("User vote %s saved to cache", vote.getVoteStatus());
            } else {
                //cache de var
                //cachedeki ve gelen statusu eynidirse
                if (statusInCache == voteRequestDto.getVoteStatus()) {
                    markedForRemoval.add("last:comment:" +  voteRequestDto.getVoteStatus().toString().toLowerCase()+ ":" + comment.getId() + ":"+ votingUser.getId());
                    markedForRemoval.expire(Duration.ofSeconds(100));
                    removeFromCache(comment, votingUser);
                    return String.format("User withdrew vote %s, this change updated to the cache", voteRequestDto.getVoteStatus());
                } else {
                    if (upVoteMap.get("last:comment:up" + comment.getId()) != null) {
                        upVoteSet = upVoteMap.get("last:comment:up" + comment.getId());
                    }
                    if (downVoteMap.get("last:comment:down" + comment.getId()) != null) {
                        downVoteSet = downVoteMap.get("last:comment:down" + comment.getId());
                    }
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        //downVoteSet.remove(votingUser.getId());
                        markedForRemoval.add("last:comment:down:" + comment.getId() + ":"+ votingUser.getId());
                        markedForRemoval.expire(Duration.ofSeconds(100));
                        removeFromCache(comment, votingUser);
                        upVoteSet.add(votingUser.getId());
                        upVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), upVoteSet);
                        upVoteMap.expire(Duration.ofMillis(100000));
                    } else {

                        markedForRemoval.add("last:comment:up:" + comment.getId() + ":"+ votingUser.getId());
                        markedForRemoval.expire(Duration.ofSeconds(100));
                        removeFromCache(comment, votingUser);
                        downVoteSet.add(votingUser.getId());
                        downVoteMap.replace("last:comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId(), downVoteSet);
                        downVoteMap.expire(Duration.ofMillis(100000));
                    }// if else etmekdense bele ede bilersen

                    return String.format("User withdrew vote %s, and voted to other. this change updated to the cache", voteRequestDto.getVoteStatus());
                }
            }
            //vote db da var upvote ve ya downvote
        } else {
            Set<Long> upVoteOnUpdateSet = new HashSet<>();
            Set<Long> downVoteOnUpdateSet = new HashSet<>();
            if (isNotInCache) {
                //remove olunmalidi bazadan
                if (optionalVote.get().getVoteStatus() == voteRequestDto.getVoteStatus()) {
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        upVoteMap.remove("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId() + ":" + votingUser.getId());

                    } else {
                        downVoteMap.remove("last:comment:" + optionalVote.get().getVoteStatus().toString().toLowerCase() + ":" + comment.getId() + ":" + votingUser.getId());
                    }
                    return String.format("User vote %s is scheduled to remove from DB", voteRequestDto.getVoteStatus());
                } else {
                    //bazadan sil yenisini yaz
                    //bazadakini silmek ucun cach-e elave edirik
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        upVoteOnUpdateSet.add(votingUser.getId());
                        upVoteMapOnUpdate.put("last:comment:up:" + commentId, upVoteOnUpdateSet);
                        upVoteMapOnUpdate.expire(Duration.ofMillis(100000));
                    } else {
                        downVoteOnUpdateSet.add(votingUser.getId());
                        downVoteMapOnUpdate.put("last:comment:down:"  + commentId, downVoteOnUpdateSet);
                        downVoteMapOnUpdate.expire(Duration.ofMillis(100000));
                    }

                    return String.format("User other vote is scheduled to remove from DB and %s is added to cache", voteRequestDto.getVoteStatus());
                }
            } else {
                if (upVoteMapOnUpdate.get("last:comment:up:" + comment.getId()) != null) {
                    upVoteOnUpdateSet = upVoteMapOnUpdate.get("last:comment:up:"  + comment.getId());
                }
                if (downVoteMapOnUpdate.get("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId()) != null) {
                    downVoteOnUpdateSet = downVoteMapOnUpdate.get("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId());
                }
                //cachededi bazadadi
                if (statusInCache == optionalVote.get().getVoteStatus()) {
                    markedForRemoval.add("last:comment:" + statusInCache.toString().toLowerCase() + ":" + comment.getId() + ":"+ votingUser.getId());
                    markedForRemoval.expire(Duration.ofSeconds(100));
                    removeFromCache(comment, votingUser);
                    if (statusInCache == VoteStatus.UP) {
                        //upVoteOnRemoveSet.remove(votingUser.getId());
                        if (voteRequestDto.getVoteStatus() == VoteStatus.DOWN) {
                            //yenisini elave et
                            downVoteOnUpdateSet.add(votingUser.getId());
                            downVoteMapOnUpdate.put("last:comment:down:" + optionalVote.get().getId() + ":" + comment.getId(), downVoteOnUpdateSet);
                            downVoteMapOnUpdate.expire(Duration.ofMillis(100000));
                            return "Vote is updated in cache from UP to DOWN";
                        }
                        return "Vote is removed from cache";
                    } else {
                        if (voteRequestDto.getVoteStatus() == VoteStatus.UP) {
                            upVoteOnUpdateSet.add(votingUser.getId());
                            upVoteMapOnUpdate.put("last:comment:up:" + comment.getId(), downVoteOnUpdateSet);
                            upVoteMapOnUpdate.expire(Duration.ofMillis(100000));
                        }
                        return "Vote is updated in cache from DOWN to UP";
                    }

                } else {
                    markedForRemoval.add("last:comment:" + statusInCache.toString().toLowerCase() + ":" + comment.getId() + ":"+ votingUser.getId());
                    markedForRemoval.expire(Duration.ofSeconds(100));
                    removeFromCache(comment, votingUser);
                    return "Vote is removed from cache";
                }
            }
        }



//axirinci querynin sebebine bir de sorusacagin sual
    }

    private void removeFromCache(Comment comment, Users votingUser) {
        if (upVoteMap != null && upVoteMap.get("last:comment:up:" + comment.getId()) != null) {
            System.out.println("Set: " + redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up:" + comment.getId()));
            Set<Long> userIdSet = upVoteMap.get("last:comment:up:" + comment.getId());
            userIdSet.remove(votingUser.getId());
            upVoteMap.replace("last:comment:up:" + comment.getId(), userIdSet);

        }
        if (downVoteMap != null && downVoteMap.get("last:comment:down:" + comment.getId()) != null) {
            Set<Long> userIdSet = downVoteMap.get("last:comment:down:" + comment.getId());
            userIdSet.remove(votingUser.getId());
            downVoteMap.replace("last:comment:down:" + comment.getId(), userIdSet);
        }

        if ( downVoteMapOnUpdate != null && downVoteMapOnUpdate.get("last:comment:down:" + comment.getId()) != null) {
            System.out.println("ON UPDATE" + downVoteMapOnUpdate.values());
            Set<Long> userIdSet = downVoteMapOnUpdate.get("last:comment:down:" + comment.getId());
            userIdSet.remove(votingUser.getId());
            downVoteMapOnUpdate.replace("last:comment:down:" + comment.getId(), userIdSet);
            //System.out.println("Comment id set " + redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()));
        }

        if (  upVoteMapOnUpdate != null && upVoteMapOnUpdate.get("last:comment:up:" + comment.getId()) != null) {
            Set<Long> userIdSet = upVoteMapOnUpdate.get("last:comment:up:" + comment.getId());
            userIdSet.remove(votingUser.getId());
            upVoteMapOnUpdate.replace("last:comment:up:" + comment.getId(), userIdSet);
        }

    }

    private Boolean initializeCacheStatus(Boolean isNotInCache, Optional<Vote> optionalVote, Comment comment, Users votingUser) {
        if (redissonClient.<String, Set<Long>>getMap("upVoteMap") != null && redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up:" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up:" + comment.getId()).contains(votingUser.getId());
        }

        if (redissonClient.<String, Set<Long>>getMap("downVoteMap") != null && redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()).contains(votingUser.getId());
        }

        if (optionalVote.isPresent() && redissonClient.<String, Set<Long>>getMap("downVoteMapOnUpdate") != null && redissonClient.<String, Set<Long>>getMap("downVoteMapOnUpdate").get("last:comment:down:" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("downVoteMapOnUpdate").get("last:comment:down:" + comment.getId()).contains(votingUser.getId());
        }

        if (optionalVote.isPresent() && redissonClient.<String, Set<Long>>getMap("upVoteMapOnUpdate") != null && redissonClient.<String, Set<Long>>getMap("upVoteMapOnUpdate").get("last:comment:up:" + comment.getId()) != null) {
            isNotInCache = !redissonClient.<String, Set<Long>>getMap("upVoteMapOnUpdate").get("last:comment:up:"+ comment.getId()).contains(votingUser.getId());

        }

        return isNotInCache;
    }

    private VoteStatus initializeCacheVoteStatus(VoteStatus statusInCache, Optional<Vote> optionalVote, Comment comment, Users votingUser) {
        if (redissonClient.<String, Set<Long>>getMap("upVoteMap") != null && redissonClient.<String, Set<Long>>getMap("upVoteMap").get("last:comment:up:" + comment.getId()) != null) {
            statusInCache = VoteStatus.UP;
        }

        if (redissonClient.<String, Set<Long>>getMap("downVoteMap") != null && redissonClient.<String, Set<Long>>getMap("downVoteMap").get("last:comment:down:" + comment.getId()) != null) {
            statusInCache = VoteStatus.DOWN;
        }

        if (optionalVote.isPresent() && redissonClient.<String, Set<Long>>getMap("downVoteMapOnUpdate") != null && redissonClient.<String, Set<Long>>getMap("downVoteMapOnUpdate").get("last:comment:down:" +comment.getId()) != null) {

            statusInCache = VoteStatus.DOWN;
        }

        if (optionalVote.isPresent() && redissonClient.<String, Set<Long>>getMap("upVoteMapOnUpdate") != null && redissonClient.<String, Set<Long>>getMap("upVoteMapOnUpdate").get("last:comment:up:" +  comment.getId()) != null) {
            statusInCache = VoteStatus.UP;
        }
        return statusInCache;
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
        comment.setContent(commentEditDto.getContent());
        commentRepository.save(comment);
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