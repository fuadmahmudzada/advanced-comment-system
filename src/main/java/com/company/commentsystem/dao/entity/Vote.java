package com.company.commentsystem.dao.entity;

import com.company.commentsystem.model.enums.VoteStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"users_id", "comment_id"}))
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Comment comment;
    @ManyToOne
    private Users users;
    @NotNull
    @Enumerated(EnumType.STRING)
    private VoteStatus voteStatus;
}

