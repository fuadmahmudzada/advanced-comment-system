package com.company.commentsystem.dao.entity;

import com.company.commentsystem.model.enums.VoteStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "comment_id"}))
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    @JsonIgnore
    private Comment comment;
    @Enumerated(EnumType.STRING)
    private VoteStatus voteStatus;
    @ManyToOne
    @JsonIgnore
    private Users user;
    @CreationTimestamp
    private LocalDateTime createdAt;
}

