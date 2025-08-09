package com.company.commentsystem.model.dto.comment_dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateDto {
    private String content;
    private Long userId;
    private Long meetingId;
    private Long repliedToId;
}
