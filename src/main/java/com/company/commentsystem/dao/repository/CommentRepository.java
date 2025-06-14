package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

}
