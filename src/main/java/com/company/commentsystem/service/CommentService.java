package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Meeting;
import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.entity.Vote;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.dao.repository.UsersRepository;
import com.company.commentsystem.dao.repository.VoteRepository;
import com.company.commentsystem.model.dto.CommentAddDto;
import com.company.commentsystem.model.dto.CommentDto;
import com.company.commentsystem.model.dto.VoteRequestDto;
import com.company.commentsystem.model.enums.VoteStatus;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLocalCachedMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public CommentDto addComment(CommentAddDto commentAddDto) {
        Comment comment = new Comment();
        Users users = new Users();
        users.setId(commentAddDto.getUserId());
        comment.setContent(commentAddDto.getContent());
        comment.setUsers(users);
        Meeting meeting = new Meeting();
        meeting.setId(commentAddDto.getMeetingId());
        comment.setMeeting(meeting);
        Comment filledComment = commentRepository.save(comment);
        CommentDto dto = new CommentDto();
        dto.setContent(filledComment.getContent());
        dto.setId(filledComment.getId());
        dto.setUserId(filledComment.getUsers().getId());
        dto.setCreatedAt(filledComment.getCreatedAt());
        return dto;

    }


    public List<CommentDto> getComments() {
        List<CommentDto> commentDtos = commentRepository.findAll().stream()
                .map(comment ->
                        new CommentDto(comment.getId(),
                                comment.getContent(),
                                comment.getCreatedAt())).toList();
        return commentDtos;
    }

    public CommentDto getCommentById(Long id) {
        Map<Object, Object> cache = redisTemplate.opsForHash().entries("newcommentapp:comment:key:" + id);
        if(!cache.isEmpty()){
            new CommentDto( cache);
        }
        Comment comment = commentRepository.findById(id).get();
        CommentDto commentDto = new CommentDto();
        commentDto.setContent(comment.getContent());
        commentDto.setId(comment.getId());
        commentDto.setCreatedAt(comment.getCreatedAt());
        commentDto.setUserId(comment.getUsers().getId());
        Map<String, CommentDto> map = new HashMap<>();

        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "id", String.valueOf(commentDto.getId()));
        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "content", String.valueOf(commentDto.getContent()));
        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "createdAt", String.valueOf(commentDto.getCreatedAt()));
        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "userId", String.valueOf(commentDto.getUserId()));


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

        if (redisTemplate.hasKey("newcommentapp:comment:key:upvote:" + id)) {
            Set<Long> upVoteSet;
            Set<Long> downVoteSet;
            if (voteRequestDto.getVoteStatus() == VoteStatus.UP) {
                upVoteSet = upVoteMap.get("newcommentapp:comment:key:" + "upVote:" + id);
                upVoteSet.add(users.getId());
                upVoteMap.fastPut("newcommentapp:comment:key:" + "upVote:" + id, upVoteSet);
            } else {
                //redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", 1);

                downVoteSet = downVoteMap.get("newcommentapp:comment:key:" + "downVote:" + id);
                downVoteSet.add(users.getId());
                downVoteMap.fastPut("newcommentapp:comment:key:" + "downVote:" + id, downVoteSet);
            }


//        redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", 1);
        } else {
            System.out.println("DIRECT SAVE HAS BEEN CALLED");
            vote.setComment(comment);
            vote.setUsers(users);

            vote.setVoteStatus(voteRequestDto.getVoteStatus());
            voteRepository.save(vote);

        }


    }
    //redisTemplate.opsForList();

}

