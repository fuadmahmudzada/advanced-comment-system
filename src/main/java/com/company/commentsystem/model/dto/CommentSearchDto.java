package com.company.commentsystem.model.dto;

import com.company.commentsystem.model.enums.CommentSearch;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentSearchDto {
    private CommentSearch commentSearch;
    private String text;
}
