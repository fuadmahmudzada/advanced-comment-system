package com.company.commentsystem.dao.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringExclude;

import java.util.List;

@Entity
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

    public Meeting() {
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setMeeting(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setMeeting(null);
    }

    public Long getId() {
        return this.id;
    }

    public @NotNull String getLink() {
        return this.link;
    }

    public List<Comment> getComments() {
        return this.comments;
    }

    public String getPlatformLink() {
        return this.platformLink;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLink(@NotNull String link) {
        this.link = link;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public void setPlatformLink(String platformLink) {
        this.platformLink = platformLink;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Meeting)) return false;
        final Meeting other = (Meeting) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$link = this.getLink();
        final Object other$link = other.getLink();
        if (this$link == null ? other$link != null : !this$link.equals(other$link)) return false;
        final Object this$comments = this.getComments();
        final Object other$comments = other.getComments();
        if (this$comments == null ? other$comments != null : !this$comments.equals(other$comments)) return false;
        final Object this$platformLink = this.getPlatformLink();
        final Object other$platformLink = other.getPlatformLink();
        if (this$platformLink == null ? other$platformLink != null : !this$platformLink.equals(other$platformLink))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Meeting;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $link = this.getLink();
        result = result * PRIME + ($link == null ? 43 : $link.hashCode());
        final Object $comments = this.getComments();
        result = result * PRIME + ($comments == null ? 43 : $comments.hashCode());
        final Object $platformLink = this.getPlatformLink();
        result = result * PRIME + ($platformLink == null ? 43 : $platformLink.hashCode());
        return result;
    }

    public String toString() {
        return "Meeting(id=" + this.getId() + ", link=" + this.getLink() +  ", platformLink=" + this.getPlatformLink() + ")";
    }
}
