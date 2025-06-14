package com.company.commentsystem.controller;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.model.dto.CommentAddDto;
import com.company.commentsystem.model.dto.CommentDto;
import com.company.commentsystem.model.enums.VoteStatus;
import com.company.commentsystem.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    //private HashOperations<String, Long, Comment> objectHashOperations =null;
    private final RedisTemplate<String, String> redisTemplate;
    private final CommentRepository commentRepository;

    //    public CommentController(RedisTemplate<String, Comment> rt, CommentService commentService)
//    {
//      //  this.objectHashOperations = rt.opsForHash();
//        this.commentService = commentService;
//    }
    @PostMapping
    public ResponseEntity<CommentDto> addComment(@RequestBody CommentAddDto commentAddDto) {

        CommentDto dto = commentService.addComment(commentAddDto);
        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        comment.setId(dto.getId());
        comment.setFullName(dto.getFullName());
        //objectHashOperations.put("COMMENTS", dto.getId(), comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public CommentDto getComment(@PathVariable Long id) {
        Comment comment = commentRepository.findById(id).get();
        CommentDto commentDto = new CommentDto();
        commentDto.setContent(comment.getContent());
        commentDto.setFullName(comment.getFullName());
        commentDto.setId(comment.getId());
        commentDto.setUpVote(comment.getUpVote());
        Map<String, CommentDto> map = new HashMap<>();

        redisTemplate.opsForHash().put("commentapp:comment:key:"+id, "id", String.valueOf(commentDto.getId()));
        redisTemplate.opsForHash().put("commentapp:comment:key:"+id, "fullName", String.valueOf(comment.getFullName()));
        redisTemplate.opsForHash().put("commentapp:comment:key:"+id, "content",String.valueOf( comment.getContent()));

        redisTemplate.opsForHash().put("commentapp:comment:key:"+id, "upVote", String.valueOf(comment.getUpVote()));
        //redisTemplate.expire("commentapp:comment:key:"+id, 50, TimeUnit.SECONDS);

//        redisTemplate.opsForHash().put("commentapp:comment:key:"+id, "upVote", comment.getUpVote());
//        redisTemplate.opsForHash().inc
        return commentDto;
    }

    @GetMapping
    //  @Cacheable(value = "commentapp:comment")
    public List<Comment> getAllComments() {
        //  List<CommentDto> list = objectHashOperations.entries("COMMENTS").values().stream().map(comment -> new CommentDto(comment.getId(), comment.getContent(), comment.getFullName(), comment.getCreatedAt())).toList();
        return commentRepository.findAll();
    }
    Jedis jedis = connectionManager.getConnection()
    @PostMapping("/{id}")
    public void vote(@PathVariable Long id, @RequestBody VoteStatus status) {
        Comment comment = commentRepository.findById(id).get();

//        Integer upVote = Integer.valueOf((String) redisTemplate.opsForHash().get("commentapp:comment:key:" + id, "upVote"));
//        upVote++;
//
//        redisTemplate.opsForHash().put("commentapp:comment:key:" + id, "upVote", String.valueOf(upVote));
        System.out.println();

        if(redisTemplate.hasKey("commentapp:comment:key:" +id)== true) {
            if (status.equals(VoteStatus.UP)) {
                redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", 1);
            } else {
                redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", -1);
            }

//        redisTemplate.opsForHash().increment("commentapp:comment:key:" + id, "upVote", 1);
        } else{
            if (status.equals(VoteStatus.UP)) {
                comment.setUpVote(comment.getUpVote() + 1);
            } else {
                comment.setUpVote(comment.getUpVote() - 1);
            }
            commentRepository.save(comment);
        }
        redisTemplate.opsForList()



    }
}
