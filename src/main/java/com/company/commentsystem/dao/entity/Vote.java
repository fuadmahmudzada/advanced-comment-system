package com.company.commentsystem.dao.entity;

import com.company.commentsystem.model.enums.VoteStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore//vote commentin conteintde get all edende useri tezeden verir
    private Users user;
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "Vote(id=" + this.getId()
                + ", voteStatus=" + this.getVoteStatus()
                + ", user=" + this.getUser()
                + ", createdAt=" + this.getCreatedAt() + ")";
    }

}

