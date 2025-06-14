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
    private String fullName;
    private LocalDateTime createdAt;
    private Integer upVote;
    public CommentDto(Long id, String content, String fullName, LocalDateTime createdAt, Integer upVote){
        this.id = id;
        this.content = content;
        this.fullName = fullName;
        this.createdAt = createdAt;
        this.upVote = upVote;
    }
    public CommentDto(){}
}
