package com.company.commentsystem.model.dto.comment_dto;

import lombok.Setter;

@Setter
public class CommentCreateResponseDto {
    private Long id;
    private String content;
    private Long upVotes;
    private Long downVotes;
}
