package com.company.commentsystem.dao.entity;

import com.company.commentsystem.model.enums.VoteStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Entity
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Comment comment;
    @ManyToOne
    private Users users;
    @NotNull
    private VoteStatus voteStatus;
}

