package com.company.commentsystem.model.dto;

import com.company.commentsystem.dao.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class CommentDto implements Serializable {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long userId;
    private Long upVote;
    private Long downVote;
    private Long repliedCommentCount;
    private List<CommentDto> replies;
    private Boolean isInSearchResult;
    public CommentDto(Long id, String content, Long upVote, Long downVote, Long userId, LocalDateTime createdAt, Long repliedCommentCount, List<CommentDto> replies){
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.upVote = upVote;
        this.downVote = downVote;
        this.userId = userId;
        this.repliedCommentCount = repliedCommentCount;
        this.replies = replies;
    }

    public CommentDto(Long id, String content, Long upVote, Long downVote, Long userId, LocalDateTime createdAt){
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.upVote = upVote;
        this.downVote = downVote;
        this.userId = userId;
    }

    public CommentDto(Long id, String content, Long upVote, Long downVote, Long userId, LocalDateTime createdAt, Long repliedCommentCount){
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.upVote = upVote;
        this.downVote = downVote;
        this.userId = userId;
        this.repliedCommentCount = repliedCommentCount;
    }
    public CommentDto(){}

    public CommentDto(Map<Object, Object> map){
        this.id = Long.valueOf((String) map.get("id"));
        this.content = (String) map.get("content");
        this.createdAt = LocalDateTime.parse((String) map.get("createdAt"));
        this.userId = Long.parseLong((String) map.get("userId"));
        this.downVote = Long.parseLong((String) map.get("downVote"));
        this.upVote = Long.parseLong((String)map.get("upVote"));
    }

}
