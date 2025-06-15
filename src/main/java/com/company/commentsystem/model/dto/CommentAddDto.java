package com.company.commentsystem.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentAddDto {
    private String content;
    private Long userId;
    private Long meetingId;
}
