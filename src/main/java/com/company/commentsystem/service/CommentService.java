package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.model.dto.CommentAddDto;
import com.company.commentsystem.model.dto.CommentDto;
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

@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RLocalCachedMap<String, Long> upVoteMap;
    private final RLocalCachedMap<String, Long> downVoteMap;

    public CommentDto addComment(CommentAddDto commentAddDto) {
        Comment comment = new Comment();
        comment.setContent(commentAddDto.getContent());
        Comment filledComment = commentRepository.save(comment);
        CommentDto dto = new CommentDto();
        dto.setContent(filledComment.getContent());
        dto.setId(filledComment.getId());

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

    public CommentDto getCommentById(Long id){
        Comment comment = commentRepository.findById(id).get();
        CommentDto commentDto = new CommentDto();
        commentDto.setContent(comment.getContent());
        commentDto.setId(comment.getId());
        Map<String, CommentDto> map = new HashMap<>();

        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "id", String.valueOf(commentDto.getId()));
        redisTemplate.opsForHash().put("newcommentapp:comment:key:" + id, "content", String.valueOf(comment.getContent()));


        upVoteMap.put("newcommentapp:comment:key:" + "upVote:" + id , voteRE);
        downVoteMap.put("newcommentapp:comment:key:"+ "downVote:" + id , commentDto.getDownVote());
        //     redisTemplate.opsForHash().put("commentapp:comment:key:"+id, "upVote", String.valueOf(comment.getUpVote()));
        //   redisTemplate.opsForHash().put("commentapp:comment:key:" +id, "downVote", String.valueOf(comment.getDownVote()));
        return commentDto;
    }

    public Long vote(Long id, VoteStatus status){
        Comment comment = commentRepository.findById(id).get();

//        Integer upVote = Integer.valueOf((String) redisTemplate.opsForHash().get("commentapp:comment:key:" + id, "upVote"));


        if (redisTemplate.hasKey("newcommentapp:comment:key:" + id) == true) {
            Long currentValue;
            if (status.equals(VoteStatus.UP)) {
                currentValue = upVoteMap.get("newcommentapp:comment:key:"+"upVote:" + id );
                upVoteMap.fastPut("newcommentapp:comment:key:"+"upVote:" + id , ++currentValue);
                //redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", 1);
            } else {
                currentValue = downVoteMap.get("newcommentapp:comment:key:"+"downVote:" + id );
                downVoteMap.fastPut("newcommentapp:comment:key:"+"downVote:" + id , ++currentValue);

            }
            return currentValue;
//        redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", 1);
        } else {
            if (status.equals(VoteStatus.UP)) {
                comment.setUpVote(comment.getUpVote() + 1);
                commentRepository.save(comment);
                return comment.getUpVote();
            } else {
                comment.setDownVote(comment.getDownVote() - 1);
                commentRepository.save(comment);
                return comment.getDownVote();
            }


        }
        //redisTemplate.opsForList();

    }
}
