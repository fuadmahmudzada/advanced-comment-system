package com.company.commentsystem.service;

import com.company.commentsystem.model.dto.comment_dto.*;
import com.company.commentsystem.model.dto.vote_dto.VoteRequestDto;
import com.company.commentsystem.model.dto.vote_dto.VoteUserDto;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CommentService {
    CommentCreateResponseDto addComment(CommentCreateDto commentCreateDto);

    ObjectNode getComments(String platformLink, Long parentId, SortType sortType, Integer pageNumber, Integer pageSize);

    //void vote(Long id, VoteRequestDto voteRequestDto);
    CommentResponseDto getCommentByIdFromDb(Long id);

    String voteFromDb(Long commentId, VoteRequestDto voteRequestDto);

    void removeFromDb(Long commentId);

    CommentResponseDto editComment(CommentEditDto commentEditDto, Long commentId);

    List<VoteUserDto> getVotes(Long commentId, VoteStatus voteStatus);

    ObjectNode searchComments(SortType sortType, int pageNumber, int pageSize, Long meetingId, CommentSearchDto commentSearchDto);

    String voteFromRedis(Long commentId, VoteRequestDto voteRequestDto);
}
