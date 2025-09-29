package com.company.commentsystem.model.dto.comment_dto;

import lombok.Getter;
import lombok.Setter;

@Setter//com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class com.company.commentsystem.model.dto.comment_dto.CommentCreateResponseDto and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS) (through reference chain: com.company.commentsystem.model.dto.response.ApiResponse["data"])
@Getter
public class CommentCreateResponseDto {
    private Long id;
    private String content;
    private Long upVotes;
    private Long downVotes;
}
