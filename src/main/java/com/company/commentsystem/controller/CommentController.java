package com.company.commentsystem.controller;

import com.company.commentsystem.model.dto.*;
import com.company.commentsystem.model.dto.comment_dto.*;
import com.company.commentsystem.model.enums.CommentSearchDeepness;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import com.company.commentsystem.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    //private HashOperations<String, Long, Comment> objectHashOperations =null;
    private final RedisTemplate<String, String> redisTemplate;


    @PostMapping
    public ResponseEntity<CommentCreateResponseDto> addComment(@RequestBody CommentCreateDto commentCreateDto) {

        CommentCreateResponseDto dto = commentService.addComment(commentCreateDto);

        //objectHashOperations.put("COMMENTS", dto.getId(), comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/comment/{id}")
    public CommentResponseDto getComment(@PathVariable Long id) {
        return commentService.getCommentByIdFromDb(id);
    }

    @GetMapping("/{platform-link}")
    public Page<CommentResponseDto> getAllComments(@PathVariable("platform-link") String platformLink, @RequestParam("sort-type") SortType sortType,
                                           @RequestParam(name = "parent-id") Long parentId, @RequestParam(name = "page-number") int pageNumber,
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable("id") Long commentId) {
        commentService.removeFromDb(commentId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CommentResponseDto> editComment(@PathVariable("id") Long commentId, @RequestBody CommentEditDto commentEditDto) {
        CommentResponseDto commentResponseDto = commentService.editComment(commentEditDto,commentId);
        return ResponseEntity.status(HttpStatus.OK).body(commentResponseDto);
    }

    @GetMapping("/{id}/votes")
    public ResponseEntity<List<VoteUserDto>> getVotes(@PathVariable(name ="id") Long commentId, @RequestParam VoteStatus voteStatus){
        return ResponseEntity.ok(commentService.getVotes(commentId, voteStatus));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CommentSearchResponseDto>> searchComments(@RequestParam(name = "page-number") int pageNumber, @RequestParam(name = "page-size") int pageSize, @RequestParam("sort-type") SortType sortType, @RequestParam Long meetingId, @RequestParam CommentSearchDeepness commentSearchDeepness, @RequestParam String content){
        return ResponseEntity.ok(commentService.searchComments(sortType, pageNumber, pageSize, meetingId, new CommentSearchDto(commentSearchDeepness, content) ));
    }
}
