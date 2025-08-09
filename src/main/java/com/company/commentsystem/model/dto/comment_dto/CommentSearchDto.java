package com.company.commentsystem.model.dto.comment_dto;

import com.company.commentsystem.model.enums.CommentSearchDeepness;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentSearchDto {
    private CommentSearchDeepness commentSearchDeepness;
    private String text;
}
