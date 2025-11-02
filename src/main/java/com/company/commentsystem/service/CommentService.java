package com.company.commentsystem.service;

import com.company.commentsystem.model.dto.comment_dto.*;
import com.company.commentsystem.model.dto.vote_dto.VoteRequestDto;
import com.company.commentsystem.model.dto.vote_dto.VoteUserDto;
import com.company.commentsystem.model.enums.SortType;
import com.company.commentsystem.model.enums.VoteStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CommentService {
    public CommentCreateResponseDto addComment(CommentCreateDto commentCreateDto);
    Page<CommentResponseDto> getComments(String platformLink, Long parentId, SortType sortType, Integer pageNumber, Integer pageSize);
    //void vote(Long id, VoteRequestDto voteRequestDto);
    CommentResponseDto getCommentByIdFromDb(Long id);

    void removeFromDb(Long commentId);
    CommentResponseDto editComment(CommentEditDto commentEditDto, Long commentId);
    List<VoteUserDto> getVotes(Long commentId, VoteStatus voteStatus);
    Page<CommentSearchResponseDto> searchComments(SortType sortType, int pageNumber, int pageSize, Long meetingId, CommentSearchDto commentSearchDto);

}
