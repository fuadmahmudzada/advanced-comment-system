package com.company.commentsystem.controller;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.entity.Vote;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.dao.repository.UsersRepository;
import com.company.commentsystem.model.dto.*;
import com.company.commentsystem.model.enums.CommentSearch;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import com.company.commentsystem.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMap;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @GetMapping("/comment/{id}")
    public CommentDto getComment(@PathVariable Long id) {
        return commentService.getCommentByIdFromDb(id);
    }

    @GetMapping("/{platformLink}")
    public Page<CommentDto> getAllComments(@PathVariable String platformLink, @RequestParam SortType sortType,
                                           @RequestHeader(name = "parent-id") Long parentId, @RequestParam(name = "page-number") int pageNumber,
                                           @RequestParam(name = "page-size") int pageSize) {
        //  List<CommentDto> list = objectHashOperations.entries("COMMENTS").values().stream().map(comment -> new CommentDto(comment.getId(), comment.getContent(), comment.getFullName(), comment.getCreatedAt())).toList();
        return commentService.getComments(platformLink, parentId, sortType, pageNumber, pageSize);
    }

    //Jedis jedis = connectionManager.getConnection()
    @PostMapping("/{id}")
    public ResponseEntity<String> vote(@PathVariable("id") Long commentId, @RequestBody VoteRequestDto voteRequestDto) {
        commentService.voteFromDb(commentId, voteRequestDto);

        return ResponseEntity.status(HttpStatus.OK).body(String.format("%s updated", voteRequestDto.getVoteStatus().toString()));
    }

    @DeleteMapping
    public ResponseEntity<String> remove(@RequestHeader(name = "comment-id") Long id) {
        commentService.removeFromDb(id);
        return ResponseEntity.ok("Comment successfully deleted");
    }

    @PatchMapping
    public ResponseEntity<CommentDto> editComment(@RequestBody CommentEditDto commentEditDto) {
        CommentDto commentDto = commentService.editComment(commentEditDto);
        return ResponseEntity.status(HttpStatus.OK).body(commentDto);
    }

    @GetMapping("/{id}/votes")
    public ResponseEntity<List<VoteUserDto>> getVotes(@PathVariable(name ="id") Long commentId, @RequestParam VoteStatus voteStatus){
        return ResponseEntity.ok(commentService.getVotes(commentId, voteStatus));
    }
//
//    @GetMapping
//    public ResponseEntity<List<CommentDto>> getAllComments(@RequestHeader(name = "comment-id") Long commentId){
//        return ResponseEntity.ok(commentService.getAllCommentsByCommentId(commentId));
//    }

    @GetMapping("/search")
    public ResponseEntity<Page<CommentDto>> searchComments(@RequestParam(name = "page-number") int pageNumber, @RequestParam(name = "page-size") int pageSize, @RequestParam SortType sortType, @RequestParam Long meetingId, @RequestParam CommentSearch commentSearch, @RequestParam String content){
        return ResponseEntity.ok(commentService.searchComments(sortType, pageNumber, pageSize, meetingId, new CommentSearchDto(commentSearch, content) ));
    }


//    @PostMapping("/post")
//    public void addsomeComment(){
//        commentService.addWithoutAdd();
//    }
//
//    @PostMapping("/post/vote")
//    public void voteWithoutAdd(){
//        commentService.voteVithoutAdd();
//    }
}
