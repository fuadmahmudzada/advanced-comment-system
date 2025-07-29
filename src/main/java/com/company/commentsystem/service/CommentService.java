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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RLocalCachedMap<String, Set<Long>> upVoteMap;
    private final RLocalCachedMap<String, Set<Long>> downVoteMap;
    private final VoteRepository voteRepository;
    private final UsersRepository usersRepository;
    private final MeetingRepository meetingRepository;

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
        repliedComment.addReply(filledComment);
        CommentDto dto = new CommentDto();
        dto.setContent(filledComment.getContent());
        dto.setId(filledComment.getId());
        dto.setUserId(filledComment.getUser().getId());
        dto.setCreatedAt(filledComment.getCreatedAt());
        return dto;
    }


    public List<CommentDto> getComments(String platformLink) {
        System.out.println(commentRepository.findAllByMeeting_PlatformLinkOrderByVoteDesc(platformLink).getFirst().getUser());
        List<CommentDto> commentDtos = commentRepository.findAllByMeeting_PlatformLinkOrderByVoteDesc(platformLink).stream()
                .map(comment ->
                        new CommentDto(comment.getId(),
                                comment.getContent(),
                                comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                                comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                                comment.getUser().getId(),
                                comment.getCreatedAt(), commentRepository.countByComment_Id(comment.getId()))).toList();
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
        return commentList.stream().map(comment ->
                new CommentDto(comment.getId(),
                        comment.getContent(),
                        comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                        comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                        comment.getUser().getId(),
                        comment.getCreatedAt(), commentRepository.countByComment_Id(comment.getId()))).toList();
    }

    public List<Comment> getAllPageableComments(SortType sortType, int pageNumber, int pageSize, Long meetingId) {

        Sort liked = JpaSort.unsafe("(select count(*) from vote where vote_status = 'UP' and comment_id = c.id) - (select count(*) from vote where vote_status = 'DOWN' and comment_id = c.id)");
        Sort sort = switch (sortType) {
            case BEST -> liked.descending();
            case NEW -> Sort.by("created_at").descending();
            case OLD -> Sort.by("created_at").ascending();
            case LEAST_LIKED -> liked.ascending();
            case TOP ->
                    JpaSort.unsafe("(select count(*) from vote where vote_status = 'UP' and comment_id = c.id)").descending();
            case HOT -> JpaSort.unsafe("""
                    log(abs((select count(*) from vote where vote_status = 'UP' and\s
                    comment_id = c.id) - (select count(*) from vote where vote_status = 'DOWN'
                    and comment_id = c.id))) + (extract(epoch from (now() - c.created_at))/4500)
                   \s""");
        };


        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        CommentSpecification commentSpecification = new CommentSpecification();
        Specification<Comment> specification = Specification.where(null);
        specification = specification.and(commentSpecification.includeReplies(CommentSearch.SUBCOMMENTS_1_DEPTH, meetingId));
        return  commentRepository.findAll(specification);
        /*commentRepository.findAllPageable(pageable, meetingId).map(comment -> {
            return new CommentDto(comment.getId(),
                    comment.getContent(),
                    comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.UP).count(),
                    comment.getVotes().stream().filter(x -> x.getVoteStatus() == VoteStatus.DOWN).count(),
                    comment.getUser().getId(),
                    comment.getCreatedAt(), commentRepository.countByComment_Id(comment.getId()));
        });*/

    }

}

