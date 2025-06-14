package com.company.commentsystem.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CommentDto implements Serializable {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    public CommentDto(Long id, String content, LocalDateTime createdAt){
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
    }
    public CommentDto(){}
}
