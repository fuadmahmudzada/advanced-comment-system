package com.company.commentsystem.dao.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringExclude;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String fullName;
    @OneToMany(mappedBy = "user", cascade = {
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.DETACH
    })
    private List<Comment> comments;

    @OneToMany(mappedBy = "user")
    private Set<Vote> votes;

    private String profilePicture;

    public Users(String fullName) {
        this.fullName = fullName;
    }

    public Users() {

    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setUser(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setUser(null);
    }

    public void addVote(Vote vote) {
        votes.add(vote);
        vote.setUser(this);
    }

    public void removeVote(Vote vote) {
        votes.remove(vote);
        vote.setUser(null);
    }


    public String toString() {
        return "Users(id=" +
                this.getId() +
                ", fullName=" +
                this.getFullName() +
                ")";
    }
}
