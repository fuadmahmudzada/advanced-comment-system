package com.company.commentsystem.model.dto.comment_dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDto {
    private Long id;
    private String content;
    private Long upVotes;
    private Long downVotes;
    private Long repliedCommentCount;
    private LocalDateTime createdAt;
}
