package com.company.commentsystem.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class CommentDto implements Serializable {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long userId;
    public CommentDto(Long id, String content, LocalDateTime createdAt){
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
    }
    public CommentDto(){}

    public CommentDto(Map<Object, Object> map){
        this.id = Long.valueOf((String) map.get("id"));
        this.content = (String) map.get("content");
        this.createdAt = LocalDateTime.parse((String) map.get("createdAt"));
        this.userId = Long.parseLong((String) map.get("userId"));
    }

}
