package com.company.commentsystem.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Setter
@Getter
@Table
@Entity(name = "comment")
public class Comment implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
    private String fullName;
    private Integer upVote = 0;
    private Long downVote;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Comment(){}
    public Comment(Map<Object, Object> map){
        this.id = Long.valueOf(Integer.valueOf((String) map.get("id")));
        this.content = (String) map.get("content");
        this.fullName = (String) map.get("fullName");
        this.upVote = Integer.valueOf((String) map.get("upVote"));
    }
}
