package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.repository.CommentRepository;
import com.company.commentsystem.model.dto.CommentAddDto;
import com.company.commentsystem.model.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;

    public CommentDto addComment(CommentAddDto commentAddDto) {
        Comment comment = new Comment();
        comment.setContent(commentAddDto.getContent());
        Comment filledComment = commentRepository.save(comment);
        CommentDto dto = new CommentDto();
        dto.setContent(filledComment.getContent());
        dto.setId(filledComment.getId());
        dto.setFullName(filledComment.getFullName());

        return dto;

    }


    public List<CommentDto> getComments() {
        List<CommentDto> commentDtos = commentRepository.findAll().stream()
                .map(comment ->
                        new CommentDto(comment.getId(),
                                comment.getContent(),
                                comment.getFullName(),
                                comment.getCreatedAt(), comment.getUpVote())).toList();
        return commentDtos;
    }
}
