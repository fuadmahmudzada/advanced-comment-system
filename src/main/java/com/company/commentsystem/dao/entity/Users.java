package com.company.commentsystem.dao.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

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
    @OneToMany
    private List<Comment> comment;
    @OneToMany
    private List<Vote> votes;

    public Users(String fullName) {
        this.fullName = fullName;
    }

    public Users() {

    }
}
