package com.company.commentsystem.model.dto;

import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.model.enums.VoteStatus;
import lombok.Data;

@Data
public class VoteRequestDto {
    private Long userId;
    private VoteStatus voteStatus;
}
