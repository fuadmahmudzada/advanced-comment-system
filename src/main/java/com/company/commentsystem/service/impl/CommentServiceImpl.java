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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
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
import java.util.*;
import java.util.stream.Collectors;

//@RequiredArgsConstructor

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RMap<String, Set<Long>> upVoteMap;
    private final RMap<String, Set<Long>> downVoteMap;
    private final RMap<String, Set<Long>> downVoteMapOnUpdate;
    private final RMap<String, Set<Long>> upVoteMapOnUpdate;
    private final RSet<String> markedForRemove;
    private final VoteRepository voteRepository;
    private final UsersRepository usersRepository;
    private final MeetingRepository meetingRepository;
    private final CommentSpecification commentSpecification;


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
    public ObjectNode getComments(String platformLink, Long parentId, SortType sortType, Integer pageNumber, Integer pageSize) {
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
        return turnPageIntoResponseStandard(comments.map(comment -> CommentMapper.INSTANCE.toCommentResponseDto(comment, commentRepository.countByParentComment_Id(comment.getId()))));

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

        Users votingUser = usersRepository.findById(voteRequestDto.getUserId()).orElseThrow(() -> new ResourceNotFoundException(String.format("User %d not found", voteRequestDto.getUserId())));

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
                    upVoteMap.put("comment:up:" + comment.getId(), upVoteSet);
                    upVoteMap.expire(Duration.ofSeconds(100));
                } else {
                    downVoteSet.add(votingUser.getId());
                    downVoteMap.put("comment:down:" + comment.getId(), downVoteSet);
                    downVoteMap.expire(Duration.ofSeconds(100));
                }
                return String.format("User vote %s saved to cache", vote.getVoteStatus());
            } else {
                //cache de var
                //cachedeki ve gelen statusu eynidirse
                if (statusInCache == voteRequestDto.getVoteStatus()) {
                    markedForRemove.add("comment:" + voteRequestDto.getVoteStatus().toString().toLowerCase() + ":" + comment.getId() + ":" + votingUser.getId());
                    markedForRemove.expire(Duration.ofSeconds(100));
                    removeFromCache(comment, votingUser);
                    return String.format("User withdrew vote %s, this change updated to the cache", voteRequestDto.getVoteStatus());
                } else {

                    upVoteSet = upVoteMap.getOrDefault("comment:up" + comment.getId(), new HashSet<>());
                    downVoteSet = downVoteMap.getOrDefault("comment:down" + comment.getId(), new HashSet<>());

                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        //downVoteSet.remove(votingUser.getId());
                        markedForRemove.add("comment:down:" + comment.getId() + ":" + votingUser.getId());
                        markedForRemove.expire(Duration.ofSeconds(100));
                        removeFromCache(comment, votingUser);
                        upVoteSet.add(votingUser.getId());
                        upVoteMap.replace("comment:up:" + comment.getId(), upVoteSet);
                        upVoteMap.expire(Duration.ofSeconds(100));
                    } else {

                        markedForRemove.add("comment:up:" + comment.getId() + ":" + votingUser.getId());
                        markedForRemove.expire(Duration.ofSeconds(100));
                        removeFromCache(comment, votingUser);
                        downVoteSet.add(votingUser.getId());
                        downVoteMap.replace("comment:down:" + comment.getId(), downVoteSet);
                        downVoteMap.expire(Duration.ofSeconds(100));
                    }// if else etmekdense bele ede bilersen

                    return String.format("User withdrew vote %s, and voted to other. this change updated to the cache", voteRequestDto.getVoteStatus());
                }
            }
            //vote db da var upvote ve ya downvote
        } else {


            if (isNotInCache) {
                //remove olunmalidi bazadan
                if (optionalVote.get().getVoteStatus() == voteRequestDto.getVoteStatus()) {
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        upVoteMap.remove("comment:up:" + comment.getId() + ":" + votingUser.getId());
                    } else {
                        downVoteMap.remove("comment:down:" + comment.getId() + ":" + votingUser.getId());
                    }
                    return String.format("User vote %s is scheduled to remove from DB", voteRequestDto.getVoteStatus());
                } else {
                    //bazadan sil yenisini yaz
                    //bazadakini silmek ucun cach-e elave edirik
                    if (voteRequestDto.getVoteStatus().equals(VoteStatus.UP)) {
                        Set<Long> upVoteOnUpdateSet = new HashSet<>();
                        upVoteOnUpdateSet.add(votingUser.getId());
                        upVoteMapOnUpdate.put("comment:up:" + commentId, upVoteOnUpdateSet);
                        upVoteMapOnUpdate.expire(Duration.ofSeconds(100));
                    } else {
                        Set<Long> downVoteOnUpdateSet = new HashSet<>();
                        downVoteOnUpdateSet.add(votingUser.getId());
                        downVoteMapOnUpdate.put("comment:down:" + commentId, downVoteOnUpdateSet);
                        downVoteMapOnUpdate.expire(Duration.ofSeconds(100));
                    }

                    return String.format("User other vote is scheduled to remove from DB and %s is added to cache", voteRequestDto.getVoteStatus());
                }
            } else {
                //cachededi bazadadi
                if (statusInCache == optionalVote.get().getVoteStatus()) {
                    markedForRemove.add("comment:" + statusInCache.toString().toLowerCase() + ":" + comment.getId() + ":" + votingUser.getId());
                    markedForRemove.expire(Duration.ofSeconds(100));
                    removeFromCache(comment, votingUser);
                    if (statusInCache == VoteStatus.UP) {
                        if (voteRequestDto.getVoteStatus() == VoteStatus.DOWN) {
                            Set<Long> downVoteOnUpdateSet = downVoteMapOnUpdate.getOrDefault("comment:down:" + optionalVote.get().getId() + ":" + comment.getId(), new HashSet<>());
                            //yenisini elave et
                            downVoteOnUpdateSet.add(votingUser.getId());
                            downVoteMapOnUpdate.put("comment:down:"+ comment.getId(), downVoteOnUpdateSet);
                            downVoteMapOnUpdate.expire(Duration.ofSeconds(100));
                            return "Vote is updated in cache from UP to DOWN";
                        }
                        return "Vote is removed from cache";
                    } else {
                        if (voteRequestDto.getVoteStatus() == VoteStatus.UP) {
                            Set<Long> upVoteOnUpdateSet = upVoteMapOnUpdate.getOrDefault("comment:up:" + comment.getId(), new HashSet<>());
                            upVoteOnUpdateSet.add(votingUser.getId());
                            upVoteMapOnUpdate.put("comment:up:" + comment.getId(), upVoteOnUpdateSet);
                            upVoteMapOnUpdate.expire(Duration.ofSeconds(100));
                        }
                        return "Vote is updated in cache from DOWN to UP";
                    }

                } else {
                    markedForRemove.add("comment:" + statusInCache.toString().toLowerCase() + ":" + comment.getId() + ":" + votingUser.getId());
                    markedForRemove.expire(Duration.ofSeconds(100));
                    removeFromCache(comment, votingUser);
                    return "Vote is removed from cache";
                }
            }
        }


//axirinci querynin sebebine bir de sorusacagin sual
    }

    private void removeFromCache(Comment comment, Users votingUser) {
        if (upVoteMap != null && upVoteMap.get("comment:up:" + comment.getId()) != null) {
            Set<Long> userIdSet = upVoteMap.get("comment:up:" + comment.getId());
            userIdSet.remove(votingUser.getId());
            upVoteMap.replace("comment:up:" + comment.getId(), userIdSet);

        }
        if (downVoteMap != null && downVoteMap.get("comment:down:" + comment.getId()) != null) {
            Set<Long> userIdSet = downVoteMap.get("comment:down:" + comment.getId());
            userIdSet.remove(votingUser.getId());
            downVoteMap.replace("comment:down:" + comment.getId(), userIdSet);
        }

        if (downVoteMapOnUpdate != null && downVoteMapOnUpdate.get("comment:down:" + comment.getId()) != null) {
            System.out.println("ON UPDATE" + downVoteMapOnUpdate.values());
            Set<Long> userIdSet = downVoteMapOnUpdate.get("comment:down:" + comment.getId());
            userIdSet.remove(votingUser.getId());
            downVoteMapOnUpdate.replace("comment:down:" + comment.getId(), userIdSet);
        }

        if (upVoteMapOnUpdate != null && upVoteMapOnUpdate.get("comment:up:" + comment.getId()) != null) {
            Set<Long> userIdSet = upVoteMapOnUpdate.get("comment:up:" + comment.getId());
            userIdSet.remove(votingUser.getId());
            upVoteMapOnUpdate.replace("comment:up:" + comment.getId(), userIdSet);
        }

    }

    private Boolean initializeCacheStatus(Boolean isNotInCache, Optional<Vote> optionalVote, Comment comment, Users votingUser) {
        if (upVoteMap != null && upVoteMap.get("comment:up:" + comment.getId()) != null) {
            isNotInCache = !upVoteMap.get("comment:up:" + comment.getId()).contains(votingUser.getId());
        }

        if (downVoteMap != null && downVoteMap.get("comment:down:" + comment.getId()) != null) {
            isNotInCache = !downVoteMap.get("comment:down:" + comment.getId()).contains(votingUser.getId());
        }

        if (optionalVote.isPresent() && downVoteMapOnUpdate != null && downVoteMapOnUpdate.get("comment:down:" + comment.getId()) != null) {
            isNotInCache = !downVoteMapOnUpdate.get("comment:down:" + comment.getId()).contains(votingUser.getId());
        }

        if (optionalVote.isPresent() && upVoteMapOnUpdate != null && upVoteMapOnUpdate.get("comment:up:" + comment.getId()) != null) {
            isNotInCache = !upVoteMapOnUpdate.get("comment:up:" + comment.getId()).contains(votingUser.getId());

        }

        return isNotInCache;
    }

    private VoteStatus initializeCacheVoteStatus(VoteStatus statusInCache, Optional<Vote> optionalVote, Comment comment, Users votingUser) {
        if (upVoteMap != null && upVoteMap.get("comment:up:" + comment.getId()) != null) {
            statusInCache = VoteStatus.UP;
        }

        if (downVoteMap != null && downVoteMap.get("comment:down:" + comment.getId()) != null) {
            statusInCache = VoteStatus.DOWN;
        }
        if (optionalVote.isPresent()) {
            if (downVoteMapOnUpdate != null && downVoteMapOnUpdate.get("comment:down:" + comment.getId()) != null) {
                statusInCache = VoteStatus.DOWN;
            }

            if (upVoteMapOnUpdate != null && upVoteMapOnUpdate.get("comment:up:" + comment.getId()) != null) {
                statusInCache = VoteStatus.UP;
            }
        }
        return statusInCache;
    }

    @Transactional
    public void removeFromDb(Long commentId) {
        if (commentRepository.countAllByParentComment_Id(commentId) == 0) {
            commentRepository.deleteById(commentId);
        } else {
            Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException(String.format("Comment %d not found", commentId)));
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
    public ObjectNode searchComments(SortType sortType, int pageNumber, int pageSize, Long
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


        return turnPageIntoResponseStandard(new PageImpl<>(searchedCommentInTreeForm.orElseThrow(() -> new ResourceNotFoundException("Search couldn't find anything")), pageable, searchedCommentInTreeForm.get().size()));
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

    private ObjectNode turnPageIntoResponseStandard(Page<CommentSearchResponseDto> commentSearchResponseDtos){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("message", "Search on comments are successful");
        objectNode.putPOJO("data", commentSearchResponseDtos.getContent());
        JsonNode metaDataObjectNode = objectMapper.valueToTree(commentSearchResponseDtos);
        ((ObjectNode) metaDataObjectNode).remove("content");
        objectNode.set("metadata", metaDataObjectNode);
        return objectNode;
    }

}