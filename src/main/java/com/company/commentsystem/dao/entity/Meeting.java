package com.company.commentsystem.dao.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@Data
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(unique = true)
    private String link;
    @OneToMany(mappedBy = "meeting", cascade = {CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.DETACH})
    private List<Comment> comments;
    private String platformLink;
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setMeeting(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setMeeting(null);
    }
}
