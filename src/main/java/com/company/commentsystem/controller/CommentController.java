package com.company.commentsystem.controller;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.dao.repository.UsersRepository;
import com.company.commentsystem.model.dto.CommentAddDto;
import com.company.commentsystem.model.dto.CommentDto;
import com.company.commentsystem.model.dto.VoteRequestDto;
import com.company.commentsystem.model.enums.VoteStatus;
import com.company.commentsystem.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMap;
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
    private final UsersRepository usersRepository;


    @PostMapping
    public ResponseEntity<CommentDto> addComment(@RequestBody CommentAddDto commentAddDto) {

        CommentDto dto = commentService.addComment(commentAddDto);

        //objectHashOperations.put("COMMENTS", dto.getId(), comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public CommentDto getComment(@PathVariable Long id) {
        return commentService.getCommentById(id);
    }

    @GetMapping
    public List<CommentDto> getAllComments() {
        //  List<CommentDto> list = objectHashOperations.entries("COMMENTS").values().stream().map(comment -> new CommentDto(comment.getId(), comment.getContent(), comment.getFullName(), comment.getCreatedAt())).toList();
        return commentService.getComments();
    }

    //Jedis jedis = connectionManager.getConnection()
    @PostMapping("/{id}")
    public ResponseEntity<String> vote(@PathVariable Long id, @RequestBody VoteRequestDto voteRequestDto) {
        commentService.vote(id, voteRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(String.format("%s updated", voteRequestDto.getVoteStatus().toString()));
    }
}
