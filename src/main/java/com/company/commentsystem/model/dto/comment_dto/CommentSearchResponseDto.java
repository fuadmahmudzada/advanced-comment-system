package com.company.commentsystem.model.dto.comment_dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
@Setter
@Getter
@AllArgsConstructor
public class CommentSearchResponseDto {
    private Long id;
    private String content;
    private Long upVotes;
    private Long downVotes;
    private LocalDateTime createdAt;
    private Boolean isInSearchResult;
    private List<CommentSearchResponseDto> replies;

    public CommentSearchResponseDto(Long id, String content, Long upVotes, Long downVotes, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.upVotes = upVotes;
        this.downVotes = downVotes;
        this.createdAt = createdAt;
    }
}
