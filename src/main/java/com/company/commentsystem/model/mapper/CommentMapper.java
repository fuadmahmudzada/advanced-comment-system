package com.company.commentsystem.model.mapper;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.model.dto.comment_dto.CommentCreateResponseDto;
import com.company.commentsystem.model.dto.comment_dto.CommentResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public abstract class CommentMapper {
    public static final CommentMapper INSTANCE = Mappers.getMapper(CommentMapper.class);

    //@Mapping(target="content")
    @Mapping(target="upVotes", expression = "java(comment.getVotes().stream().filter(x -> x.getVoteStatus() == com.company.commentsystem.model.enums.VoteStatus.UP).count())")
    @Mapping(target="downVotes", expression = "java(comment.getVotes().stream().filter(x -> x.getVoteStatus() == com.company.commentsystem.model.enums.VoteStatus.DOWN).count())")
    public abstract CommentCreateResponseDto toCommentCreateResponseDto(Comment comment);

    @Mapping(target="upVotes", expression = "java(comment.getVotes().stream().filter(x -> x.getVoteStatus() == com.company.commentsystem.model.enums.VoteStatus.UP).count())")
    @Mapping(target="downVotes", expression = "java(comment.getVotes().stream().filter(x -> x.getVoteStatus() == com.company.commentsystem.model.enums.VoteStatus.DOWN).count())")
    public abstract CommentResponseDto toCommentResponseDto(Comment comment, Long repliedCommentCount);

    public abstract CommentResponseDto toCommentResponseDto(Comment comment, Long upVotes, Long downVotes, Long repliedCommentCount);
}
