package com.company.commentsystem.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
@Table
@Entity
//@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
//@NamedNativeQuery(query = "select count(*) from vote v  where v.voteStatus = 'UP' and v.commentId = :commentId", name = "findUpVoteCount")
public class Comment implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    //@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Users user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    @JsonIgnore
//    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Meeting meeting;
    @OneToMany(mappedBy = "comment", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnore
    private Set<Vote> votes = new HashSet<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
//    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Comment parentComment;
    @Transient
    private Boolean isInSearchResult = false;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;


    public Comment() {
    }

    public Comment(Map<Object, Object> map) {
        this.id = Long.valueOf(Integer.valueOf((String) map.get("id")));
        this.content = (String) map.get("content");
    }


    public void addVote(Vote vote) {
        votes.add(vote);
        vote.setComment(this);
    }

    public void removeVote(Vote vote) {
        votes.remove(vote);
        vote.setComment(null);
    }

    public String toString() {
        return "Comment(id=" + this.getId() +
                ", content=" + this.getContent() +
                ", user=" + this.getUser() +
                ", meeting=" + this.getMeeting() +
                ", votes=" + this.getVotes() +

                ", isInSearchResult=" + this.getIsInSearchResult() +
                ", createdAt=" + this.getCreatedAt() +
                ", updatedAt=" + this.getUpdatedAt() + ")";
    }

//    @PostLoad
//    private void fillUpVotes(){
//        this.upVotes = entityManager.
//
//    }


}
