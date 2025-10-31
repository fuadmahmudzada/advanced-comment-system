package com.company.commentsystem.controller;

import com.company.commentsystem.utils.ResponseUtil;
import com.company.commentsystem.model.dto.comment_dto.*;
import com.company.commentsystem.model.dto.response.ApiResponse;
import com.company.commentsystem.model.dto.vote_dto.VoteRequestDto;
import com.company.commentsystem.model.dto.vote_dto.VoteUserDto;
import com.company.commentsystem.model.enums.CommentSearchDeepness;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import com.company.commentsystem.service.impl.CommentServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    private final CommentServiceImpl commentServiceImpl;
    //private HashOperations<String, Long, Comment> objectHashOperations =null;
    private final RedisTemplate<String, String> redisTemplate;


    @PostMapping
    public ResponseEntity<ApiResponse<CommentCreateResponseDto>> addComment(@RequestBody CommentCreateDto commentCreateDto) {

        CommentCreateResponseDto dto = commentServiceImpl.addComment(commentCreateDto);

        //objectHashOperations.put("COMMENTS", dto.getId(), comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success("Comment added", dto, null));
    }

    @GetMapping("/comment/{id}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> getComment(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseUtil.success("Comment fetched", commentServiceImpl.getCommentByIdFromDb(id), null));
    }

    @GetMapping("/{platform-link}")
    public ResponseEntity<ObjectNode> getAllComments(@PathVariable("platform-link") String platformLink, @RequestParam("sort-type") SortType sortType,
                                                     @RequestParam(name = "parent-id") Long parentId, @RequestParam(name = "page-number") int pageNumber,
                                                     @RequestParam(name = "page-size") int pageSize)  {
        //  List<CommentDto> list = objectHashOperations.entries("COMMENTS").values().stream().map(comment -> new CommentDto(comment.getId(), comment.getContent(), comment.getFullName(), comment.getCreatedAt())).toList();
        Page<CommentResponseDto> page = commentServiceImpl.getComments(platformLink, parentId, sortType, pageNumber, pageSize);
        List<CommentResponseDto> list = page.getContent();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();

        objectMapper.registerModule(new JavaTimeModule());
        objectNode.putNull("message");
        objectNode.put("status", "success");
        objectNode.putPOJO("data", list);
        JsonNode metadata =  objectMapper.valueToTree(page);
        ((ObjectNode)metadata).remove("content");
        objectNode.set("metadata", metadata);
        // page.getContent().removeAll(page.getContent());

        return ResponseEntity.ok(objectNode
        );
    }

    //Jedis jedis = connectionManager.getConnection()
    @PostMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> vote(@PathVariable("id") Long commentId, @RequestBody VoteRequestDto voteRequestDto) {
        String message = commentServiceImpl.voteFromRedis(commentId, voteRequestDto);

        return ResponseEntity.status(HttpStatus.OK).body(ResponseUtil.success(String.format(message, voteRequestDto.getVoteStatus()), null, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable("id") Long commentId) {
        commentServiceImpl.removeFromDb(commentId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> editComment(@PathVariable("id") Long commentId, @RequestBody CommentEditDto commentEditDto) {
        CommentResponseDto commentResponseDto = commentServiceImpl.editComment(commentEditDto, commentId);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseUtil.success("Comment edited", commentResponseDto, null));
    }

    @GetMapping("/{id}/votes")
    public ResponseEntity<ApiResponse<List<VoteUserDto>>> getVotes(@PathVariable(name = "id") Long commentId, @RequestParam VoteStatus voteStatus) {
        return ResponseEntity.ok(ResponseUtil.success(null, commentServiceImpl.getVotes(commentId, voteStatus), null));
    }

    @GetMapping("/search")
    public ResponseEntity<ObjectNode> searchComments(@RequestParam(name = "page-number") int pageNumber, @RequestParam(name = "page-size") int pageSize, @RequestParam("sort-type") SortType sortType, @RequestParam Long meetingId, @RequestParam CommentSearchDeepness commentSearchDeepness, @RequestParam String content) throws JsonProcessingException {
        Page<CommentSearchResponseDto> commentSearchResponseDtos = commentServiceImpl.searchComments(sortType, pageNumber, pageSize, meetingId, new CommentSearchDto(commentSearchDeepness, content));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("message", "Search on comments are successful");
        objectNode.putPOJO("data", commentSearchResponseDtos.getContent());
        JsonNode metaDataObjectNode = objectMapper.valueToTree(commentSearchResponseDtos);
        ((ObjectNode) metaDataObjectNode).remove("content");
        objectNode.set("metadata", metaDataObjectNode);

        return ResponseEntity.ok(objectNode);
    }
}
